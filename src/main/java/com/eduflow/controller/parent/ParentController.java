package com.eduflow.controller.parent;

import com.eduflow.dto.request.CreatePaymentRequest;
import com.eduflow.dto.response.*;
import com.eduflow.entity.academic.AssessmentScore;
import com.eduflow.entity.academic.Parent;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.communication.Announcement;
import com.eduflow.entity.communication.AnnouncementRead;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.AssessmentScoreRepository;
import com.eduflow.repository.academic.ParentRepository;
import com.eduflow.repository.communication.AnnouncementReadRepository;
import com.eduflow.repository.communication.AnnouncementRepository;
import com.eduflow.repository.communication.NotificationRepository;
import com.eduflow.repository.finance.PaymentRepository;
import com.eduflow.repository.finance.StudentFeeAssignmentRepository;
import com.eduflow.service.FeeService;
import com.eduflow.service.PaymentService;
import com.eduflow.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final StudentFeeAssignmentRepository feeAssignmentRepository;
    private final PaymentRepository paymentRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Get parent dashboard", description = "Get dashboard summary for parent")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);
        List<Student> children = parent.getChildren().stream().toList();

        int totalChildren = children.size();
        BigDecimal totalFeesDue = BigDecimal.ZERO;
        BigDecimal totalFeesPaid = BigDecimal.ZERO;
        int overdueCount = 0;

        for (Student child : children) {
            var fees = feeAssignmentRepository.findByStudentId(child.getId());
            for (var fee : fees) {
                totalFeesDue = totalFeesDue.add(fee.getNetAmount());
                // Calculate actual payments for this fee
                BigDecimal feePaid = paymentRepository.calculateTotalPaidByFeeAssignmentId(fee.getId());
                BigDecimal feeBalance = fee.getNetAmount().subtract(feePaid);
                if (fee.getDueDate() != null && fee.getDueDate().isBefore(java.time.LocalDate.now())
                        && feeBalance.compareTo(BigDecimal.ZERO) > 0) {
                    overdueCount++;
                }
            }
            // Get actual completed payments for this student
            totalFeesPaid = totalFeesPaid.add(paymentRepository.calculateTotalPaidByStudentId(child.getId()));
        }

        BigDecimal outstandingBalance = totalFeesDue.subtract(totalFeesPaid);
        long unreadNotifications = notificationRepository.countUnreadByRecipientId(parent.getUser().getId());

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalChildren", totalChildren);
        dashboard.put("totalFeesDue", totalFeesDue);
        dashboard.put("totalFeesPaid", totalFeesPaid);
        dashboard.put("outstandingBalance", outstandingBalance);
        dashboard.put("overdueCount", overdueCount);
        dashboard.put("unreadNotifications", unreadNotifications);

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/children")
    @Operation(summary = "Get children", description = "Get all children associated with the parent with fee summaries")
    public ResponseEntity<List<StudentResponse>> getChildren(@AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);
        List<StudentResponse> children = studentService.getStudentsByParentId(parent.getId());

        // Enrich each child with fee summary using actual payment data
        for (StudentResponse child : children) {
            var fees = feeAssignmentRepository.findByStudentId(child.getId());
            BigDecimal totalFees = BigDecimal.ZERO;

            for (var fee : fees) {
                totalFees = totalFees.add(fee.getNetAmount());
            }

            // Get actual completed payments for this student
            BigDecimal totalPaid = paymentRepository.calculateTotalPaidByStudentId(child.getId());
            BigDecimal balance = totalFees.subtract(totalPaid);

            // Count pending fees (where balance > 0)
            int pendingCount = 0;
            for (var fee : fees) {
                BigDecimal feePaid = paymentRepository.calculateTotalPaidByFeeAssignmentId(fee.getId());
                if (fee.getNetAmount().subtract(feePaid).compareTo(BigDecimal.ZERO) > 0) {
                    pendingCount++;
                }
            }

            child.setFeeSummary(StudentResponse.FeeSummary.builder()
                    .totalFees(totalFees)
                    .totalPaid(totalPaid)
                    .balance(balance)
                    .pendingFees(pendingCount)
                    .build());
        }

        return ResponseEntity.ok(children);
    }

    @GetMapping("/children/{studentId}")
    @Operation(summary = "Get child details", description = "Get details of a specific child")
    public ResponseEntity<StudentResponse> getChildById(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyParentAccessToStudent(userDetails, studentId);
        return ResponseEntity.ok(studentService.getStudentById(studentId));
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

    @GetMapping("/children/{childId}/grades")
    @Operation(summary = "Get child grades", description = "Get assessment scores for a specific child with optional date range")
    public ResponseEntity<ChildGradesResponse> getChildGrades(
            @PathVariable Long childId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyParentAccessToStudent(userDetails, childId);

        List<AssessmentScore> scores;
        if (startDate != null && endDate != null) {
            scores = assessmentScoreRepository.findByStudentIdAndDateRange(childId, startDate, endDate);
        } else {
            scores = assessmentScoreRepository.findByStudentIdWithDetails(childId);
        }

        // Calculate summary
        int totalAssessments = scores.size();
        int absences = (int) scores.stream().filter(s -> Boolean.TRUE.equals(s.getAbsent())).count();

        BigDecimal overallAverage = null;
        List<AssessmentScore> scoredAssessments = scores.stream()
                .filter(s -> s.getScore() != null && !Boolean.TRUE.equals(s.getAbsent()))
                .toList();

        if (!scoredAssessments.isEmpty()) {
            BigDecimal totalPercentage = scoredAssessments.stream()
                    .map(s -> s.getScore()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(s.getAssessment().getMaxScore(), 2, java.math.RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            overallAverage = totalPercentage.divide(
                    BigDecimal.valueOf(scoredAssessments.size()), 2, java.math.RoundingMode.HALF_UP);
        }

        List<ChildGradesResponse.GradeItem> gradeItems = scores.stream()
                .map(s -> {
                    BigDecimal percentage = null;
                    if (s.getScore() != null && !Boolean.TRUE.equals(s.getAbsent())) {
                        percentage = s.getScore()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(s.getAssessment().getMaxScore(), 1, java.math.RoundingMode.HALF_UP);
                    }

                    return ChildGradesResponse.GradeItem.builder()
                            .id(s.getId())
                            .assessment(ChildGradesResponse.AssessmentInfo.builder()
                                    .id(s.getAssessment().getId())
                                    .title(s.getAssessment().getTitle())
                                    .type(s.getAssessment().getType().name())
                                    .date(s.getAssessment().getDate())
                                    .maxScore(s.getAssessment().getMaxScore())
                                    .term(s.getAssessment().getTerm().name())
                                    .academicYear(s.getAssessment().getAcademicYear())
                                    .build())
                            .subject(ChildGradesResponse.SubjectInfo.builder()
                                    .id(s.getAssessment().getSubject().getId())
                                    .name(s.getAssessment().getSubject().getName())
                                    .build())
                            .score(s.getScore())
                            .percentage(percentage)
                            .absent(s.getAbsent())
                            .remarks(s.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ChildGradesResponse.builder()
                .totalAssessments(totalAssessments)
                .overallAverage(overallAverage)
                .absences(absences)
                .grades(gradeItems)
                .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChildGradesResponse {
        private int totalAssessments;
        private BigDecimal overallAverage;
        private int absences;
        private List<GradeItem> grades;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class GradeItem {
            private Long id;
            private AssessmentInfo assessment;
            private SubjectInfo subject;
            private BigDecimal score;
            private BigDecimal percentage;
            private Boolean absent;
            private String remarks;
        }

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class AssessmentInfo {
            private Long id;
            private String title;
            private String type;
            private LocalDate date;
            private BigDecimal maxScore;
            private String term;
            private String academicYear;
        }

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class SubjectInfo {
            private Long id;
            private String name;
        }
    }

    @GetMapping("/children/{studentId}/payments")
    @Operation(summary = "Get payment history for child", description = "Get payment history for a specific child")
    public ResponseEntity<PagedResponse<PaymentResponse>> getChildPaymentHistory(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyParentAccessToStudent(userDetails, studentId);
        return ResponseEntity.ok(paymentService.getPaymentsByStudentId(studentId, pageable));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get all payments", description = "Get payment history for all children")
    public ResponseEntity<PagedResponse<PaymentResponse>> getAllPayments(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);
        List<Long> childIds = parent.getChildren().stream()
                .map(Student::getId)
                .toList();
        return ResponseEntity.ok(paymentService.getPaymentsByStudentIds(childIds, pageable));
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

    // Announcement endpoints
    @GetMapping("/announcements")
    @Operation(summary = "Get announcements", description = "Get all announcements for parents including class-specific ones")
    public ResponseEntity<PagedResponse<AnnouncementResponse>> getAnnouncements(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Parent parent = getParentFromUser(userDetails);
        Long userId = parent.getUser().getId();

        // Get children's class IDs for class-targeted announcements
        List<Long> childClassIds = parent.getChildren().stream()
                .filter(child -> child.getCurrentClass() != null)
                .map(child -> child.getCurrentClass().getId())
                .collect(Collectors.toList());

        // If no classes, use empty list to avoid query issues
        if (childClassIds.isEmpty()) {
            childClassIds = List.of(0L);
        }

        // Get announcements targeted to ALL, PARENTS, specific classes, or this user
        Page<Announcement> page = announcementRepository.findAnnouncementsForParent(
                childClassIds, userId, pageable);

        List<Long> readIds = announcementReadRepository.findReadAnnouncementIdsByUserId(userId);

        List<AnnouncementResponse> content = page.getContent().stream()
                .map(a -> mapToAnnouncementResponse(a, readIds.contains(a.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/announcements/unread-count")
    @Operation(summary = "Get unread announcements count", description = "Get count of unread announcements")
    public ResponseEntity<Long> getUnreadAnnouncementCount(@AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);
        Long userId = parent.getUser().getId();

        // Get children's class IDs
        List<Long> childClassIds = parent.getChildren().stream()
                .filter(child -> child.getCurrentClass() != null)
                .map(child -> child.getCurrentClass().getId())
                .collect(Collectors.toList());

        if (childClassIds.isEmpty()) {
            childClassIds = List.of(0L);
        }

        return ResponseEntity.ok(announcementRepository.countUnreadForParent(childClassIds, userId));
    }

    @PostMapping("/announcements/{id}/read")
    @Operation(summary = "Mark announcement as read", description = "Mark a specific announcement as read")
    public ResponseEntity<MessageResponse> markAnnouncementAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Parent parent = getParentFromUser(userDetails);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        // Check if already read
        if (!announcementReadRepository.existsByAnnouncementIdAndUserId(id, parent.getUser().getId())) {
            AnnouncementRead read = AnnouncementRead.builder()
                    .announcement(announcement)
                    .user(parent.getUser())
                    .readAt(LocalDateTime.now())
                    .build();
            announcementReadRepository.save(read);
        }

        return ResponseEntity.ok(MessageResponse.success("Announcement marked as read"));
    }

    private AnnouncementResponse mapToAnnouncementResponse(Announcement announcement, boolean read) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .priority(announcement.getPriority() != null ? announcement.getPriority().name() : null)
                .publishedAt(announcement.getPublishedAt())
                .expiresAt(announcement.getExpiresAt())
                .attachments(announcement.getAttachments())
                .read(read)
                .senderName(announcement.getSender() != null ? announcement.getSender().getFullName() : null)
                .build();
    }

    private Parent getParentFromUser(UserDetails userDetails) {
        return parentRepository.findByUserEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "email", userDetails.getUsername()));
    }

    private void verifyParentAccessToStudent(UserDetails userDetails, Long studentId) {
        Parent parent = getParentFromUser(userDetails);
        boolean hasAccess = parent.getChildren().stream()
                .anyMatch(s -> s.getId().equals(studentId));
        if (!hasAccess) {
            throw new ResourceNotFoundException("Student not found or access denied");
        }
    }
}
