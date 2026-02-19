package com.eduflow.service;

import com.eduflow.dto.request.CreatePaymentRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.PaymentResponse;
import com.eduflow.entity.finance.Payment;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {

    PaymentResponse initiatePayment(CreatePaymentRequest request);

    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByTransactionRef(String transactionRef);

    PagedResponse<PaymentResponse> getPaymentsByStudentId(Long studentId, Pageable pageable);

    PagedResponse<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status, Pageable pageable);

    List<PaymentResponse> getPaymentsByFeeAssignment(Long feeAssignmentId);

    PaymentResponse processPaymentCallback(String gatewayRef, boolean success, String responseData);

    PaymentResponse cancelPayment(Long paymentId, String reason);
}
