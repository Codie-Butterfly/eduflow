package com.eduflow.controller.admin;

import com.eduflow.dto.response.DashboardStatsResponse;
import com.eduflow.repository.academic.SchoolClassRepository;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.academic.TeacherRepository;
import com.eduflow.repository.finance.PaymentRepository;
import com.eduflow.repository.finance.StudentFeeAssignmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "Dashboard statistics endpoints")
public class AdminDashboardController {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository classRepository;
    private final PaymentRepository paymentRepository;
    private final StudentFeeAssignmentRepository feeAssignmentRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", description = "Get counts and totals for admin dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalClasses = classRepository.count();
        long totalPayments = paymentRepository.count();
        long pendingPayments = paymentRepository.countPendingPayments();

        BigDecimal feesCollected = paymentRepository.calculateTotalCollected();
        if (feesCollected == null) {
            feesCollected = BigDecimal.ZERO;
        }

        BigDecimal outstandingFees = feeAssignmentRepository.calculateTotalOutstandingFees();
        if (outstandingFees == null) {
            outstandingFees = BigDecimal.ZERO;
        }

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .feesCollected(feesCollected)
                .outstandingFees(outstandingFees)
                .totalPayments(totalPayments)
                .pendingPayments(pendingPayments)
                .build();

        return ResponseEntity.ok(stats);
    }
}
