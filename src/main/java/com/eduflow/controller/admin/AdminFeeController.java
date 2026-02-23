package com.eduflow.controller.admin;

import com.eduflow.dto.request.AssignFeeRequest;
import com.eduflow.dto.request.CreateFeeRequest;
import com.eduflow.dto.response.FeeResponse;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.StudentFeeResponse;
import com.eduflow.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/admin/fees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Fees", description = "Fee management endpoints")
public class AdminFeeController {

    private final FeeService feeService;

    @GetMapping
    @Operation(summary = "List all fees", description = "Get all fee structures")
    public ResponseEntity<List<FeeResponse>> getAllFees() {
        return ResponseEntity.ok(feeService.getAllFees());
    }

    @GetMapping("/assignments")
    @Operation(summary = "List all fee assignments", description = "Get all student fee assignments")
    public ResponseEntity<List<StudentFeeResponse>> getAllFeeAssignments() {
        return ResponseEntity.ok(feeService.getAllFeeAssignments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fee by ID", description = "Get fee structure details")
    public ResponseEntity<FeeResponse> getFeeById(@PathVariable Long id) {
        return ResponseEntity.ok(feeService.getFeeById(id));
    }

    @GetMapping("/academic-year/{year}")
    @Operation(summary = "Get fees by academic year", description = "Get all fees for a specific academic year")
    public ResponseEntity<List<FeeResponse>> getFeesByAcademicYear(@PathVariable String year) {
        return ResponseEntity.ok(feeService.getFeesByAcademicYear(year));
    }

    @PostMapping
    @Operation(summary = "Create fee", description = "Create a new fee structure")
    public ResponseEntity<FeeResponse> createFee(@Valid @RequestBody CreateFeeRequest request) {
        return ResponseEntity.ok(feeService.createFee(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update fee", description = "Update an existing fee structure")
    public ResponseEntity<FeeResponse> updateFee(
            @PathVariable Long id,
            @Valid @RequestBody CreateFeeRequest request) {
        return ResponseEntity.ok(feeService.updateFee(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete fee", description = "Deactivate a fee structure")
    public ResponseEntity<MessageResponse> deleteFee(@PathVariable Long id) {
        feeService.deleteFee(id);
        return ResponseEntity.ok(MessageResponse.success("Fee deactivated successfully"));
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign fees to students", description = "Assign fees to students or classes")
    public ResponseEntity<List<StudentFeeResponse>> assignFees(@Valid @RequestBody AssignFeeRequest request) {
        return ResponseEntity.ok(feeService.assignFeesToStudents(request));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get student fees", description = "Get all fees assigned to a student")
    public ResponseEntity<List<StudentFeeResponse>> getStudentFees(@PathVariable Long studentId) {
        return ResponseEntity.ok(feeService.getStudentFees(studentId));
    }

    @GetMapping("/student/{studentId}/year/{academicYear}")
    @Operation(summary = "Get student fees by year", description = "Get student fees for a specific academic year")
    public ResponseEntity<List<StudentFeeResponse>> getStudentFeesByYear(
            @PathVariable Long studentId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(feeService.getStudentFeesByYear(studentId, academicYear));
    }

    @PostMapping("/assignment/{id}/discount")
    @Operation(summary = "Apply discount", description = "Apply discount to a fee assignment")
    public ResponseEntity<StudentFeeResponse> applyDiscount(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {
        return ResponseEntity.ok(feeService.applyDiscount(id, amount, reason));
    }

    @PostMapping("/assignment/{id}/waive")
    @Operation(summary = "Waive fee", description = "Waive a fee assignment completely")
    public ResponseEntity<StudentFeeResponse> waiveFee(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(feeService.waiveFee(id, reason));
    }
}
