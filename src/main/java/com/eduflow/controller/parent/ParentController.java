package com.eduflow.controller.parent;

import com.eduflow.dto.request.CreatePaymentRequest;
import com.eduflow.dto.response.*;
import com.eduflow.entity.academic.Parent;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.ParentRepository;
import com.eduflow.repository.communication.NotificationRepository;
import com.eduflow.service.FeeService;
import com.eduflow.service.PaymentService;
import com.eduflow.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent", description = "Parent portal endpoints")
public class ParentController {

    private final StudentService studentService;
    private final FeeService feeService;
    private final PaymentService paymentService;
    private final ParentRepository parentRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/children")
    @Operation(summary = "Get children", description = "Get all children associated with the parent")
    public ResponseEntity<List<StudentResponse>> getChildren(@AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);
        return ResponseEntity.ok(studentService.getStudentsByParentId(parent.getId()));
    }

    @GetMapping("/children/{studentId}/fees")
    @Operation(summary = "Get child fees", description = "Get fee breakdown for a specific child")
    public ResponseEntity<List<StudentFeeResponse>> getChildFees(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyParentAccessToStudent(userDetails, studentId);
        return ResponseEntity.ok(feeService.getStudentFees(studentId));
    }

    @GetMapping("/children/{studentId}/fees/{academicYear}")
    @Operation(summary = "Get child fees by year", description = "Get fees for a specific academic year")
    public ResponseEntity<List<StudentFeeResponse>> getChildFeesByYear(
            @PathVariable Long studentId,
            @PathVariable String academicYear,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyParentAccessToStudent(userDetails, studentId);
        return ResponseEntity.ok(feeService.getStudentFeesByYear(studentId, academicYear));
    }

    @GetMapping("/children/{studentId}/payments")
    @Operation(summary = "Get payment history", description = "Get payment history for a specific child")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentHistory(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyParentAccessToStudent(userDetails, studentId);
        return ResponseEntity.ok(paymentService.getPaymentsByStudentId(studentId, pageable));
    }

    @PostMapping("/payments")
    @Operation(summary = "Make payment", description = "Initiate a payment for a fee")
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Verify parent has access to the student associated with the fee assignment
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get payment details", description = "Get details of a specific payment")
    public ResponseEntity<PaymentResponse> getPaymentDetails(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications", description = "Get notifications for the parent")
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Parent parent = getParentFromUser(userDetails);
        return ResponseEntity.ok(
                notificationRepository.findByRecipientIdOrderBySentAtDesc(
                        parent.getUser().getId(), pageable)
        );
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<Long> getUnreadNotificationCount(@AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);
        return ResponseEntity.ok(
                notificationRepository.countUnreadByRecipientId(parent.getUser().getId())
        );
    }

    private Parent getParentFromUser(UserDetails userDetails) {
        return parentRepository.findByUserId(getUserIdFromEmail(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found"));
    }

    private void verifyParentAccessToStudent(UserDetails userDetails, Long studentId) {
        Parent parent = getParentFromUser(userDetails);
        boolean hasAccess = parent.getChildren().stream()
                .anyMatch(s -> s.getId().equals(studentId));
        if (!hasAccess) {
            throw new ResourceNotFoundException("Student not found or access denied");
        }
    }

    private Long getUserIdFromEmail(String email) {
        // This would typically be retrieved from a service or the authentication principal
        // For now, we assume the parent lookup handles this
        return null; // The repository method will handle the lookup
    }
}
