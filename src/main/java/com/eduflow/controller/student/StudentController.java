package com.eduflow.controller.student;

import com.eduflow.dto.response.*;
import com.eduflow.entity.academic.AssessmentScore;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.communication.Announcement;
import com.eduflow.entity.communication.AnnouncementRead;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.AssessmentScoreRepository;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.communication.AnnouncementReadRepository;
import com.eduflow.repository.communication.AnnouncementRepository;
import com.eduflow.repository.communication.NotificationRepository;
import com.eduflow.repository.finance.PaymentRepository;
import com.eduflow.repository.finance.StudentFeeAssignmentRepository;
import com.eduflow.service.FeeService;
import com.eduflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student", description = "Student portal endpoints")
public class StudentController {

    private final StudentRepository studentRepository;
    private final FeeService feeService;
    private final PaymentService paymentService;
    private final StudentFeeAssignmentRepository feeAssignmentRepository;
    private final PaymentRepository paymentRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final NotificationRepository notificationRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get student profile", description = "Get the logged-in student's profile")
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", student.getId());
        profile.put("studentId", student.getStudentId());
        profile.put("firstName", student.getUser().getFirstName());
        profile.put("lastName", student.getUser().getLastName());
        profile.put("fullName", student.getUser().getFullName());
        profile.put("email", student.getUser().getEmail());
        profile.put("phone", student.getUser().getPhone());
        profile.put("dateOfBirth", student.getDateOfBirth());
        profile.put("gender", student.getGender());
        profile.put("address", student.getAddress());
        profile.put("enrollmentDate", student.getEnrollmentDate());
        profile.put("status", student.getStatus());
        profile.put("bloodGroup", student.getBloodGroup());

        if (student.getCurrentClass() != null) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("id", student.getCurrentClass().getId());
            classInfo.put("name", student.getCurrentClass().getName());
            classInfo.put("grade", student.getCurrentClass().getGrade());
            classInfo.put("section", student.getCurrentClass().getSection());
            profile.put("currentClass", classInfo);
        }

        if (student.getParent() != null) {
            Map<String, Object> parentInfo = new HashMap<>();
            parentInfo.put("id", student.getParent().getId());
            parentInfo.put("name", student.getParent().getUser().getFullName());
            parentInfo.put("phone", student.getParent().getUser().getPhone());
            parentInfo.put("email", student.getParent().getUser().getEmail());
            profile.put("parent", parentInfo);
        }

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get student dashboard", description = "Get dashboard summary for student")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);

        // Fee summary
        var fees = feeAssignmentRepository.findByStudentId(student.getId());
        BigDecimal totalFees = BigDecimal.ZERO;
        int pendingFees = 0;
        int overdueFees = 0;

        for (var fee : fees) {
            totalFees = totalFees.add(fee.getNetAmount());
            BigDecimal feePaid = paymentRepository.calculateTotalPaidByFeeAssignmentId(fee.getId());
            BigDecimal balance = fee.getNetAmount().subtract(feePaid);
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                pendingFees++;
                if (fee.getDueDate() != null && fee.getDueDate().isBefore(LocalDate.now())) {
                    overdueFees++;
                }
            }
        }

        BigDecimal totalPaid = paymentRepository.calculateTotalPaidByStudentId(student.getId());
        BigDecimal outstandingBalance = totalFees.subtract(totalPaid);

        // Academic summary
        List<AssessmentScore> recentScores = assessmentScoreRepository.findByStudentIdWithDetails(student.getId());
        int totalAssessments = recentScores.size();
        BigDecimal averageScore = null;

        List<AssessmentScore> scoredAssessments = recentScores.stream()
                .filter(s -> s.getScore() != null && !Boolean.TRUE.equals(s.getAbsent()))
                .toList();

        if (!scoredAssessments.isEmpty()) {
            BigDecimal totalPercentage = scoredAssessments.stream()
                    .map(s -> s.getScore()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(s.getAssessment().getMaxScore(), 2, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageScore = totalPercentage.divide(
                    BigDecimal.valueOf(scoredAssessments.size()), 2, RoundingMode.HALF_UP);
        }

        // Notifications
        long unreadNotifications = notificationRepository.countUnreadByRecipientId(student.getUser().getId());

        // Class info
        String className = student.getCurrentClass() != null ? student.getCurrentClass().getName() : null;

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("studentId", student.getStudentId());
        dashboard.put("studentName", student.getUser().getFullName());
        dashboard.put("className", className);

        // Fees
        Map<String, Object> feesSummary = new HashMap<>();
        feesSummary.put("totalFees", totalFees);
        feesSummary.put("totalPaid", totalPaid);
        feesSummary.put("outstandingBalance", outstandingBalance);
        feesSummary.put("pendingFees", pendingFees);
        feesSummary.put("overdueFees", overdueFees);
        dashboard.put("fees", feesSummary);

        // Academic
        Map<String, Object> academicSummary = new HashMap<>();
        academicSummary.put("totalAssessments", totalAssessments);
        academicSummary.put("averageScore", averageScore);
        dashboard.put("academic", academicSummary);

        dashboard.put("unreadNotifications", unreadNotifications);

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/fees")
    @Operation(summary = "Get student fees", description = "Get fee breakdown for the logged-in student")
    public ResponseEntity<List<StudentFeeResponse>> getFees(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);
        return ResponseEntity.ok(feeService.getStudentFees(student.getId()));
    }

    @GetMapping("/fees/{academicYear}")
    @Operation(summary = "Get fees by academic year", description = "Get fees for a specific academic year")
    public ResponseEntity<List<StudentFeeResponse>> getFeesByYear(
            @PathVariable String academicYear,
            @AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);
        return ResponseEntity.ok(feeService.getStudentFeesByYear(student.getId(), academicYear));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get payment history", description = "Get payment history for the logged-in student")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPayments(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);
        return ResponseEntity.ok(paymentService.getPaymentsByStudentId(student.getId(), pageable));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get payment details", description = "Get details of a specific payment")
    public ResponseEntity<PaymentResponse> getPaymentDetails(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // TODO: Add validation that payment belongs to this student
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/grades")
    @Operation(summary = "Get grades", description = "Get assessment scores for the logged-in student")
    public ResponseEntity<GradesResponse> getGrades(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);

        List<AssessmentScore> scores;
        if (startDate != null && endDate != null) {
            scores = assessmentScoreRepository.findByStudentIdAndDateRange(student.getId(), startDate, endDate);
        } else {
            scores = assessmentScoreRepository.findByStudentIdWithDetails(student.getId());
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
                            .divide(s.getAssessment().getMaxScore(), 2, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            overallAverage = totalPercentage.divide(
                    BigDecimal.valueOf(scoredAssessments.size()), 2, RoundingMode.HALF_UP);
        }

        List<GradesResponse.GradeItem> gradeItems = scores.stream()
                .map(s -> {
                    BigDecimal percentage = null;
                    if (s.getScore() != null && !Boolean.TRUE.equals(s.getAbsent())) {
                        percentage = s.getScore()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(s.getAssessment().getMaxScore(), 1, RoundingMode.HALF_UP);
                    }

                    return GradesResponse.GradeItem.builder()
                            .id(s.getId())
                            .assessment(GradesResponse.AssessmentInfo.builder()
                                    .id(s.getAssessment().getId())
                                    .title(s.getAssessment().getTitle())
                                    .type(s.getAssessment().getType().name())
                                    .date(s.getAssessment().getDate())
                                    .maxScore(s.getAssessment().getMaxScore())
                                    .term(s.getAssessment().getTerm().name())
                                    .academicYear(s.getAssessment().getAcademicYear())
                                    .build())
                            .subject(GradesResponse.SubjectInfo.builder()
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

        return ResponseEntity.ok(GradesResponse.builder()
                .totalAssessments(totalAssessments)
                .overallAverage(overallAverage)
                .absences(absences)
                .grades(gradeItems)
                .build());
    }

    @GetMapping("/announcements")
    @Operation(summary = "Get announcements", description = "Get announcements for the student")
    public ResponseEntity<PagedResponse<AnnouncementResponse>> getAnnouncements(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Student student = getStudentFromUser(userDetails);
        Long userId = student.getUser().getId();
        Long classId = student.getCurrentClass() != null ? student.getCurrentClass().getId() : 0L;

        Page<Announcement> page = announcementRepository.findAnnouncementsForStudent(classId, userId, pageable);
        List<Long> readIds = announcementReadRepository.findReadAnnouncementIdsByUserId(userId);

        List<AnnouncementResponse> content = page.getContent().stream()
                .map(a -> mapToAnnouncementResponse(a, readIds.contains(a.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @PostMapping("/announcements/{id}/read")
    @Operation(summary = "Mark announcement as read", description = "Mark a specific announcement as read")
    public ResponseEntity<MessageResponse> markAnnouncementAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Student student = getStudentFromUser(userDetails);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!announcementReadRepository.existsByAnnouncementIdAndUserId(id, student.getUser().getId())) {
            AnnouncementRead read = AnnouncementRead.builder()
                    .announcement(announcement)
                    .user(student.getUser())
                    .readAt(LocalDateTime.now())
                    .build();
            announcementReadRepository.save(read);
        }

        return ResponseEntity.ok(MessageResponse.success("Announcement marked as read"));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications", description = "Get notifications for the student")
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Student student = getStudentFromUser(userDetails);
        return ResponseEntity.ok(
                notificationRepository.findByRecipientIdOrderBySentAtDesc(
                        student.getUser().getId(), pageable)
        );
    }

    private Student getStudentFromUser(UserDetails userDetails) {
        return studentRepository.findByUserEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "email", userDetails.getUsername()));
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

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GradesResponse {
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
}