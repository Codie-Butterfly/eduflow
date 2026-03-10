package com.eduflow.controller.admin;

import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.PaymentResponse;
import com.eduflow.entity.finance.Payment;
import com.eduflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Payments", description = "Payment management endpoints")
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "List all payments", description = "Get paginated list of all payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status", description = "Get payments filtered by status")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByStatus(
            @PathVariable Payment.PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status, pageable));
    }
}
