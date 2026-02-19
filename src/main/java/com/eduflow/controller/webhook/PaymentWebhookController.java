package com.eduflow.controller.webhook;

import com.eduflow.dto.request.PaymentWebhookRequest;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.entity.finance.Payment;
import com.eduflow.entity.finance.PaymentTransaction;
import com.eduflow.exception.BadRequestException;
import com.eduflow.repository.finance.PaymentRepository;
import com.eduflow.repository.finance.PaymentTransactionRepository;
import com.eduflow.security.hmac.HmacSignatureVerifier;
import com.eduflow.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Payment gateway webhook endpoints")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final HmacSignatureVerifier hmacVerifier;
    private final ObjectMapper objectMapper;

    private static final long WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS = 300; // 5 minutes

    @PostMapping("/payment")
    @Operation(summary = "Payment webhook", description = "Receive payment confirmation from gateway")
    public ResponseEntity<MessageResponse> handlePaymentWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) Long timestamp,
            @RequestBody String rawPayload
    ) {
        log.info("Received payment webhook");

        // Verify timestamp to prevent replay attacks
        if (timestamp != null && !hmacVerifier.verifyTimestamp(timestamp, WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS)) {
            log.warn("Webhook timestamp verification failed");
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Invalid timestamp"));
        }

        // Verify HMAC signature
        if (signature != null && !hmacVerifier.verifySignature(rawPayload, signature)) {
            log.warn("Webhook signature verification failed");
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Invalid signature"));
        }

        try {
            PaymentWebhookRequest webhookData = objectMapper.readValue(rawPayload, PaymentWebhookRequest.class);

            // Check for idempotency - prevent duplicate processing
            if (transactionRepository.existsByGatewayRef(webhookData.getTransactionId())) {
                log.info("Webhook already processed for transaction: {}", webhookData.getTransactionId());
                return ResponseEntity.ok(MessageResponse.success("Already processed"));
            }

            // Find the payment
            Payment payment = paymentRepository.findByGatewayRef(webhookData.getGatewayReference())
                    .orElse(null);

            if (payment == null) {
                // Try by merchant reference
                payment = paymentRepository.findByTransactionRef(webhookData.getMerchantReference())
                        .orElse(null);
            }

            if (payment == null) {
                log.warn("Payment not found for webhook: gateway_ref={}, merchant_ref={}",
                        webhookData.getGatewayReference(), webhookData.getMerchantReference());
                return ResponseEntity.ok(MessageResponse.error("Payment not found"));
            }

            // Log the webhook transaction
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .payment(payment)
                    .transactionType(PaymentTransaction.TransactionType.WEBHOOK)
                    .gatewayRef(webhookData.getTransactionId())
                    .webhookPayload(rawPayload)
                    .processedAt(LocalDateTime.now())
                    .build();

            // Process based on event type
            boolean success = processWebhookEvent(webhookData, payment, transaction);

            transactionRepository.save(transaction);

            if (success) {
                paymentService.processPaymentCallback(
                        webhookData.getGatewayReference(),
                        "SUCCESS".equalsIgnoreCase(webhookData.getStatus()) ||
                                "COMPLETED".equalsIgnoreCase(webhookData.getStatus()),
                        rawPayload
                );
            }

            log.info("Webhook processed successfully for payment: {}", payment.getTransactionRef());
            return ResponseEntity.ok(MessageResponse.success("Webhook processed"));

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("Processing failed"));
        }
    }

    private boolean processWebhookEvent(PaymentWebhookRequest webhookData, Payment payment,
                                        PaymentTransaction transaction) {
        String eventType = webhookData.getEventType();
        String status = webhookData.getStatus();

        log.info("Processing webhook event: type={}, status={}", eventType, status);

        switch (eventType != null ? eventType.toUpperCase() : "") {
            case "PAYMENT.SUCCESS", "PAYMENT.COMPLETED" -> {
                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                return true;
            }
            case "PAYMENT.FAILED", "PAYMENT.DECLINED" -> {
                transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                transaction.setErrorMessage(webhookData.getFailureReason());
                return true;
            }
            case "PAYMENT.PENDING" -> {
                transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
                return false;
            }
            default -> {
                // Handle status-based fallback
                if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                    transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                    return true;
                } else if ("FAILED".equalsIgnoreCase(status) || "DECLINED".equalsIgnoreCase(status)) {
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    return true;
                }
                log.warn("Unknown webhook event type: {}", eventType);
                transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
                return false;
            }
        }
    }
}
