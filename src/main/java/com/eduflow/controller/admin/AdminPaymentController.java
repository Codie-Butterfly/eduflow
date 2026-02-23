package com.eduflow.controller.admin;

import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.PaymentResponse;
import com.eduflow.entity.finance.Payment;
import com.eduflow.repository.finance.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Payments", description = "Payment management endpoints")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping
    @Operation(summary = "List all payments", description = "Get paginated list of all payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "paidAt") Pageable pageable) {
        Page<Payment> page = paymentRepository.findAll(pageable);
        List<PaymentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status", description = "Get payments filtered by status")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByStatus(
            @PathVariable Payment.PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Payment> page = paymentRepository.findByStatus(status, pageable);
        List<PaymentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse.StudentInfo student = null;
        PaymentResponse.FeeInfo fee = null;

        if (payment.getStudentFeeAssignment() != null) {
            var assignment = payment.getStudentFeeAssignment();
            if (assignment.getStudent() != null && assignment.getStudent().getUser() != null) {
                student = PaymentResponse.StudentInfo.builder()
                        .id(assignment.getStudent().getId())
                        .studentId(assignment.getStudent().getStudentId())
                        .name(assignment.getStudent().getUser().getFullName())
                        .build();
            }
            if (assignment.getFee() != null) {
                fee = PaymentResponse.FeeInfo.builder()
                        .id(assignment.getFee().getId())
                        .name(assignment.getFee().getName())
                        .category(assignment.getFee().getCategory() != null ?
                                assignment.getFee().getCategory().getName().name() : null)
                        .build();
            }
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionRef(payment.getTransactionRef())
                .gatewayRef(payment.getGatewayRef())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .payerName(payment.getPayerName())
                .payerPhone(payment.getPayerPhone())
                .payerEmail(payment.getPayerEmail())
                .notes(payment.getNotes())
                .failureReason(payment.getFailureReason())
                .student(student)
                .fee(fee)
                .build();
    }
}
