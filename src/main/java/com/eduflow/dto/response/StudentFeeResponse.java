package com.eduflow.dto.response;

import com.eduflow.entity.finance.FeeCategory;
import com.eduflow.entity.finance.StudentFeeAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeResponse {

    private Long id;
    private String feeName;
    private FeeCategory.CategoryType category;
    private String academicYear;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private String discountReason;
    private BigDecimal netAmount;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    private StudentFeeAssignment.FeeStatus status;
    private StudentSummary student;
    private List<PaymentSummary> payments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private Long id;
        private String studentId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String className;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private Long id;
        private BigDecimal amount;
        private String paymentMethod;
        private String transactionRef;
        private String status;
        private String paidAt;
    }
}
