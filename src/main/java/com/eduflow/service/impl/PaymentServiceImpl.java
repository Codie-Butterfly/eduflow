package com.eduflow.service.impl;

import com.eduflow.dto.request.CreatePaymentRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.PaymentResponse;
import com.eduflow.entity.finance.Payment;
import com.eduflow.entity.finance.PaymentTransaction;
import com.eduflow.entity.finance.StudentFeeAssignment;
import com.eduflow.exception.BadRequestException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.finance.PaymentRepository;
import com.eduflow.repository.finance.PaymentTransactionRepository;
import com.eduflow.repository.finance.StudentFeeAssignmentRepository;
import com.eduflow.service.PaymentService;
import com.eduflow.service.payment.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final StudentFeeAssignmentRepository feeAssignmentRepository;
    private final PaymentGatewayService paymentGatewayService;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request) {
        StudentFeeAssignment feeAssignment = feeAssignmentRepository.findById(request.getStudentFeeAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee Assignment", "id", request.getStudentFeeAssignmentId()));

        BigDecimal balance = feeAssignment.getBalance();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Fee is already fully paid");
        }

        if (request.getAmount().compareTo(balance) > 0) {
            throw new BadRequestException("Payment amount exceeds remaining balance");
        }

        String transactionRef = generateTransactionRef();

        Payment payment = Payment.builder()
                .studentFeeAssignment(feeAssignment)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(transactionRef)
                .status(Payment.PaymentStatus.PENDING)
                .payerName(request.getPayerName())
                .payerPhone(request.getPayerPhone())
                .payerEmail(request.getPayerEmail())
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);

        // For cash payments, complete immediately
        if (request.getPaymentMethod() == Payment.PaymentMethod.CASH) {
            return completeCashPayment(payment);
        }

        // For electronic payments, initiate gateway transaction
        try {
            String gatewayRef = paymentGatewayService.initiatePayment(payment);
            payment.setGatewayRef(gatewayRef);
            payment.setStatus(Payment.PaymentStatus.PROCESSING);

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .payment(payment)
                    .transactionType(PaymentTransaction.TransactionType.INITIATION)
                    .gatewayRef(gatewayRef)
                    .status(PaymentTransaction.TransactionStatus.SUCCESS)
                    .processedAt(LocalDateTime.now())
                    .build();

            payment.addTransaction(transaction);
            payment = paymentRepository.save(payment);

            log.info("Payment initiated: {} - Gateway ref: {}", transactionRef, gatewayRef);
        } catch (Exception e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            payment = paymentRepository.save(payment);
        }

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionRef(String transactionRef) {
        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionRef", transactionRef));
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsByStudentId(Long studentId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByStudentId(studentId, pageable);
        List<PaymentResponse> content = page.getContent().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByStatus(status, pageable);
        List<PaymentResponse> content = page.getContent().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByFeeAssignment(Long feeAssignmentId) {
        return paymentRepository.findByStudentFeeAssignmentId(feeAssignmentId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponse processPaymentCallback(String gatewayRef, boolean success, String responseData) {
        Payment payment = paymentRepository.findByGatewayRef(gatewayRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "gatewayRef", gatewayRef));

        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(payment)
                .transactionType(PaymentTransaction.TransactionType.CALLBACK)
                .gatewayRef(gatewayRef)
                .gatewayResponse(responseData)
                .processedAt(LocalDateTime.now())
                .build();

        if (success) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);

            // Update fee assignment
            StudentFeeAssignment feeAssignment = payment.getStudentFeeAssignment();
            feeAssignment.addPayment(payment.getAmount());
            feeAssignmentRepository.save(feeAssignment);

            log.info("Payment completed: {} - Amount: {}", payment.getTransactionRef(), payment.getAmount());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined by gateway");
            transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);

            log.warn("Payment failed: {}", payment.getTransactionRef());
        }

        payment.addTransaction(transaction);
        payment = paymentRepository.save(payment);

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed payment");
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setFailureReason(reason);
        payment = paymentRepository.save(payment);

        log.info("Payment cancelled: {} - Reason: {}", payment.getTransactionRef(), reason);
        return mapToPaymentResponse(payment);
    }

    private PaymentResponse completeCashPayment(Payment payment) {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        StudentFeeAssignment feeAssignment = payment.getStudentFeeAssignment();
        feeAssignment.addPayment(payment.getAmount());
        feeAssignmentRepository.save(feeAssignment);

        payment = paymentRepository.save(payment);
        log.info("Cash payment completed: {} - Amount: {}", payment.getTransactionRef(), payment.getAmount());

        return mapToPaymentResponse(payment);
    }

    private String generateTransactionRef() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        StudentFeeAssignment assignment = payment.getStudentFeeAssignment();

        PaymentResponse.StudentInfo studentInfo = PaymentResponse.StudentInfo.builder()
                .id(assignment.getStudent().getId())
                .studentId(assignment.getStudent().getStudentId())
                .name(assignment.getStudent().getUser().getFullName())
                .build();

        PaymentResponse.FeeInfo feeInfo = PaymentResponse.FeeInfo.builder()
                .id(assignment.getFee().getId())
                .name(assignment.getFee().getName())
                .category(assignment.getFee().getCategory().getName().name())
                .build();

        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionRef(payment.getTransactionRef())
                .gatewayRef(payment.getGatewayRef())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .payerName(payment.getPayerName())
                .payerPhone(payment.getPayerPhone())
                .payerEmail(payment.getPayerEmail())
                .notes(payment.getNotes())
                .failureReason(payment.getFailureReason())
                .student(studentInfo)
                .fee(feeInfo)
                .build();
    }
}
