package com.eduflow.service.payment;

import com.eduflow.entity.finance.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Value("${payment.gateway.base-url}")
    private String gatewayBaseUrl;

    @Value("${payment.gateway.api-key}")
    private String apiKey;

    @Value("${payment.gateway.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String initiatePayment(Payment payment) {
        log.info("Initiating payment with gateway: amount={}, method={}",
                payment.getAmount(), payment.getPaymentMethod());

        // In production, this would make actual API calls to the payment gateway
        // For now, we'll simulate the gateway response

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", payment.getAmount());
            requestBody.put("currency", "ZMW");
            requestBody.put("reference", payment.getTransactionRef());
            requestBody.put("payment_method", mapPaymentMethod(payment.getPaymentMethod()));
            requestBody.put("customer_phone", payment.getPayerPhone());
            requestBody.put("customer_email", payment.getPayerEmail());
            requestBody.put("customer_name", payment.getPayerName());
            requestBody.put("callback_url", getCallbackUrl());

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Simulated gateway reference - in production this comes from the actual API response
            String gatewayRef = "GW" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

            log.info("Payment gateway reference generated: {}", gatewayRef);
            return gatewayRef;

            // Actual API call would be:
            // ResponseEntity<Map> response = restTemplate.postForEntity(
            //     gatewayBaseUrl + "/payments/initiate",
            //     request,
            //     Map.class
            // );
            // return (String) response.getBody().get("gateway_reference");

        } catch (Exception e) {
            log.error("Failed to initiate payment with gateway: {}", e.getMessage());
            throw new RuntimeException("Payment gateway error: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPayment(String gatewayRef) {
        log.info("Verifying payment with gateway: {}", gatewayRef);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // In production:
            // ResponseEntity<Map> response = restTemplate.exchange(
            //     gatewayBaseUrl + "/payments/" + gatewayRef + "/verify",
            //     HttpMethod.GET,
            //     request,
            //     Map.class
            // );
            // return "SUCCESS".equals(response.getBody().get("status"));

            // Simulated verification
            return true;

        } catch (Exception e) {
            log.error("Failed to verify payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void processRefund(String gatewayRef, BigDecimal amount) {
        log.info("Processing refund for payment: {} amount: {}", gatewayRef, amount);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("gateway_reference", gatewayRef);
            requestBody.put("amount", amount);
            requestBody.put("reason", "Customer requested refund");

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // In production:
            // restTemplate.postForEntity(
            //     gatewayBaseUrl + "/payments/refund",
            //     request,
            //     Map.class
            // );

            log.info("Refund processed successfully for: {}", gatewayRef);

        } catch (Exception e) {
            log.error("Failed to process refund: {}", e.getMessage());
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    private String mapPaymentMethod(Payment.PaymentMethod method) {
        return switch (method) {
            case MOBILE_MONEY_MTN -> "mtn_momo";
            case MOBILE_MONEY_AIRTEL -> "airtel_money";
            case MOBILE_MONEY_ZAMTEL -> "zamtel_money";
            case VISA -> "visa";
            case MASTERCARD -> "mastercard";
            case BANK_TRANSFER -> "bank_transfer";
            default -> "other";
        };
    }

    private String getCallbackUrl() {
        // This should be configured based on environment
        return "https://api.eduflow.com/api/v1/webhooks/payment";
    }
}
