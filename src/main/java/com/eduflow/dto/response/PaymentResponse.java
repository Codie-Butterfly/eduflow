package com.eduflow.dto.response;

import com.eduflow.entity.finance.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private BigDecimal amount;
    private Payment.PaymentMethod paymentMethod;
    private String transactionRef;
    private String gatewayRef;
    private Payment.PaymentStatus status;
    private LocalDateTime paidAt;
    private String payerName;
    private String payerPhone;
    private String payerEmail;
    private String notes;
    private String failureReason;

    private StudentInfo student;
    private FeeInfo fee;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private Long id;
        private String studentId;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeInfo {
        private Long id;
        private String name;
        private String category;
    }
}
