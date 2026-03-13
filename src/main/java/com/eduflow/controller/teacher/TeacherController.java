package com.eduflow.controller.teacher;

import com.eduflow.dto.request.CreateAssessmentRequest;
import com.eduflow.dto.request.CreateGradeRequest;
import com.eduflow.dto.request.CreateHomeworkRequest;
import com.eduflow.dto.response.*;
import com.eduflow.entity.academic.Assessment;
import com.eduflow.entity.academic.AssessmentScore;
import com.eduflow.entity.academic.Attendance;
import com.eduflow.entity.academic.TeacherClassSubject;
import com.eduflow.entity.academic.Grade;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.entity.communication.Announcement;
import com.eduflow.entity.communication.AnnouncementRead;
import com.eduflow.entity.communication.Homework;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.*;
import com.eduflow.repository.communication.AnnouncementReadRepository;
import com.eduflow.repository.communication.AnnouncementRepository;
import com.eduflow.repository.communication.HomeworkRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher", description = "Teacher portal endpoints")
public class TeacherController {

    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final HomeworkRepository homeworkRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Get teacher dashboard", description = "Get teacher dashboard summary")
    public ResponseEntity<java.util.Map<String, Object>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);

        List<SchoolClass> classes = classRepository.findByClassTeacherId(teacher.getId());
        int totalStudents = classes.stream()
                .mapToInt(c -> c.getStudents().size())
                .sum();

        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();
        dashboard.put("teacherId", teacher.getId());
        dashboard.put("employeeId", teacher.getEmployeeId());
        dashboard.put("name", teacher.getUser().getFullName());
        dashboard.put("email", teacher.getUser().getEmail());
        dashboard.put("totalClasses", classes.size());
        dashboard.put("totalStudents", totalStudents);
        dashboard.put("subjects", teacher.getSubjects().size());

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/classes")
    @Operation(summary = "Get assigned classes", description = "Get classes assigned to the teacher")
    public ResponseEntity<List<ClassResponse>> getAssignedClasses(
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);

        List<SchoolClass> classes = classRepository.findByClassTeacherId(teacher.getId());
        List<ClassResponse> response = classes.stream()
                .map(this::mapToClassResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/classes/{classId}")
    @Operation(summary = "Get class by ID", description = "Get details of a specific class")
    public ResponseEntity<ClassResponse> getClassById(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserDetails userDetails) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        return ResponseEntity.ok(mapToClassResponse(schoolClass));
    }

    @GetMapping("/classes/{classId}/students")
    @Operation(summary = "Get students in class", description = "Get all students in a specific class")
    public ResponseEntity<List<StudentResponse>> getStudentsInClass(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<com.eduflow.entity.academic.Student> students = studentRepository.findByCurrentClassId(classId);
        List<StudentResponse> response = students.stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/grades")
    @Operation(summary = "Add grade", description = "Add a grade for a student")
    public ResponseEntity<GradeResponse> addGrade(
            @Valid @RequestBody CreateGradeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);

        var enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", request.getEnrollmentId()));

        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));

        // Check if grade already exists
        var existingGrade = gradeRepository.findByEnrollmentIdAndSubjectIdAndTerm(
                request.getEnrollmentId(), request.getSubjectId(), request.getTerm());

        Grade grade;
        if (existingGrade.isPresent()) {
            // Update existing grade
            grade = existingGrade.get();
            grade.setScore(request.getScore());
            grade.setMaxScore(request.getMaxScore());
            grade.setTeacherComment(request.getTeacherComment());
        } else {
            // Create new grade
            grade = Grade.builder()
                    .enrollment(enrollment)
                    .subject(subject)
                    .score(request.getScore())
                    .maxScore(request.getMaxScore())
                    .term(request.getTerm())
                    .academicYear(request.getAcademicYear())
                    .teacherComment(request.getTeacherComment())
                    .gradedBy(teacher)
                    .build();
        }

        // Calculate grade letter
        grade.setGradeLetter(calculateGradeLetter(request.getScore(), request.getMaxScore()));
        grade = gradeRepository.save(grade);

        return ResponseEntity.ok(mapToGradeResponse(grade));
    }

    @GetMapping("/grades")
    @Operation(summary = "Get grades", description = "Get grades entered by the teacher")
    public ResponseEntity<List<GradeResponse>> getGrades(
            @RequestParam String academicYear,
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);
        List<Grade> grades = gradeRepository.findByTeacherIdAndAcademicYear(teacher.getId(), academicYear);

        List<GradeResponse> response = grades.stream()
                .map(this::mapToGradeResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/homework")
    @Operation(summary = "Post homework", description = "Create a new homework assignment")
    public ResponseEntity<HomeworkResponse> postHomework(
            @Valid @RequestBody CreateHomeworkRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);

        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));

        var schoolClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.getClassId()));

        Homework homework = Homework.builder()
                .teacher(teacher)
                .subject(subject)
                .schoolClass(schoolClass)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .attachments(request.getAttachments() != null ? request.getAttachments() : List.of())
                .maxScore(request.getMaxScore())
                .status(Homework.HomeworkStatus.ACTIVE)
                .academicYear(request.getAcademicYear())
                .term(request.getTerm())
                .build();

        homework = homeworkRepository.save(homework);
        return ResponseEntity.ok(mapToHomeworkResponse(homework));
    }

    @GetMapping("/homework")
    @Operation(summary = "Get homework", description = "Get homework created by the teacher")
    public ResponseEntity<List<HomeworkResponse>> getHomework(
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);
        List<Homework> homeworkList = homeworkRepository.findActiveByTeacherId(teacher.getId());

        List<HomeworkResponse> response = homeworkList.stream()
                .map(this::mapToHomeworkResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // Teacher Assignment endpoints - get classes and subjects teacher is assigned to
    @GetMapping("/my-assignments")
    @Operation(summary = "Get my teaching assignments", description = "Get classes and subjects assigned to the teacher")
    public ResponseEntity<List<TeacherAssignmentResponse.ClassWithSubjects>> getMyAssignments(
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);
        log.info("GET /my-assignments - Request: teacherId={}, user={}", teacher.getId(), userDetails.getUsername());

        List<TeacherClassSubject> assignments = teacherClassSubjectRepository.findByTeacherIdAndActiveTrue(teacher.getId());

        // Group by class
        java.util.Map<Long, List<TeacherClassSubject>> byClass = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getSchoolClass().getId()));

        List<TeacherAssignmentResponse.ClassWithSubjects> response = byClass.entrySet().stream()
                .map(entry -> {
                    TeacherClassSubject first = entry.getValue().get(0);
                    SchoolClass sc = first.getSchoolClass();
                    List<TeacherAssignmentResponse.SubjectInfo> subjects = entry.getValue().stream()
                            .map(a -> TeacherAssignmentResponse.SubjectInfo.builder()
                                    .id(a.getSubject().getId())
                                    .name(a.getSubject().getName())
                                    .code(a.getSubject().getCode())
                                    .build())
                            .collect(Collectors.toList());

                    return TeacherAssignmentResponse.ClassWithSubjects.builder()
                            .classId(sc.getId())
                            .className(sc.getName())
                            .grade(sc.getGrade())
                            .section(sc.getSection())
                            .studentCount(sc.getStudents().size())
                            .subjects(subjects)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("GET /my-assignments - Response: {} classes found", response.size());
        return ResponseEntity.ok(response);
    }

    // Assessment endpoints
    @PostMapping("/assessments")
    @Operation(summary = "Create assessment", description = "Create a new test/exercise and optionally record scores")
    public ResponseEntity<AssessmentResponse> createAssessment(
            @Valid @RequestBody CreateAssessmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);
        log.info("POST /assessments - Request: title={}, type={}, classId={}, subjectId={}, date={}, maxScore={}, user={}",
                request.getTitle(), request.getType(), request.getClassId(), request.getSubjectId(),
                request.getDate(), request.getMaxScore(), userDetails.getUsername());

        SchoolClass schoolClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.getClassId()));

        com.eduflow.entity.academic.Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));

        Assessment assessment = Assessment.builder()
                .title(request.getTitle())
                .type(request.getType())
                .teacher(teacher)
                .schoolClass(schoolClass)
                .subject(subject)
                .date(request.getDate())
                .maxScore(request.getMaxScore())
                .term(request.getTerm())
                .academicYear(request.getAcademicYear())
                .description(request.getDescription())
                .build();

        assessment = assessmentRepository.save(assessment);

        // If scores are provided, save them
        if (request.getScores() != null && !request.getScores().isEmpty()) {
            for (CreateAssessmentRequest.StudentScore scoreReq : request.getScores()) {
                com.eduflow.entity.academic.Student student = studentRepository.findById(scoreReq.getStudentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student", "id", scoreReq.getStudentId()));

                AssessmentScore score = AssessmentScore.builder()
                        .assessment(assessment)
                        .student(student)
                        .score(scoreReq.getScore())
                        .remarks(scoreReq.getRemarks())
                        .absent(scoreReq.getAbsent() != null ? scoreReq.getAbsent() : false)
                        .build();
                assessmentScoreRepository.save(score);
            }
            log.info("POST /assessments - Saved {} scores", request.getScores().size());
        }

        log.info("POST /assessments - Response: assessmentId={}", assessment.getId());
        return ResponseEntity.ok(mapToAssessmentResponse(assessment, true));
    }

    @GetMapping("/assessments")
    @Operation(summary = "Get my assessments", description = "Get assessments created by the teacher")
    public ResponseEntity<List<AssessmentResponse>> getMyAssessments(
            @RequestParam(required = false) String academicYear,
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);
        log.info("GET /assessments - Request: teacherId={}, academicYear={}, user={}",
                teacher.getId(), academicYear, userDetails.getUsername());

        List<Assessment> assessments;
        if (academicYear != null) {
            assessments = assessmentRepository.findByTeacherIdAndAcademicYearOrderByDateDesc(teacher.getId(), academicYear);
        } else {
            assessments = assessmentRepository.findByTeacherIdOrderByDateDesc(teacher.getId());
        }

        List<AssessmentResponse> response = assessments.stream()
                .map(a -> mapToAssessmentResponse(a, false))
                .collect(Collectors.toList());

        log.info("GET /assessments - Response: {} assessments found", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assessments/{assessmentId}")
    @Operation(summary = "Get assessment details", description = "Get assessment with all student scores")
    public ResponseEntity<AssessmentResponse> getAssessment(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /assessments/{} - Request: user={}", assessmentId, userDetails.getUsername());

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", assessmentId));

        AssessmentResponse response = mapToAssessmentResponse(assessment, true);
        log.info("GET /assessments/{} - Response: {} scores", assessmentId, response.getScores().size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assessments/{assessmentId}/scores")
    @Operation(summary = "Record scores", description = "Record or update student scores for an assessment")
    public ResponseEntity<AssessmentResponse> recordScores(
            @PathVariable Long assessmentId,
            @Valid @RequestBody List<CreateAssessmentRequest.StudentScore> scores,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /assessments/{}/scores - Request: {} students, user={}",
                assessmentId, scores.size(), userDetails.getUsername());

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", assessmentId));

        for (CreateAssessmentRequest.StudentScore scoreReq : scores) {
            com.eduflow.entity.academic.Student student = studentRepository.findById(scoreReq.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", scoreReq.getStudentId()));

            AssessmentScore score = assessmentScoreRepository.findByAssessmentIdAndStudentId(assessmentId, scoreReq.getStudentId())
                    .orElse(AssessmentScore.builder()
                            .assessment(assessment)
                            .student(student)
                            .build());

            score.setScore(scoreReq.getScore());
            score.setRemarks(scoreReq.getRemarks());
            score.setAbsent(scoreReq.getAbsent() != null ? scoreReq.getAbsent() : false);
            assessmentScoreRepository.save(score);
        }

        log.info("POST /assessments/{}/scores - Response: {} scores saved", assessmentId, scores.size());
        return ResponseEntity.ok(mapToAssessmentResponse(assessment, true));
    }

    @GetMapping("/classes/{classId}/assessments")
    @Operation(summary = "Get class assessments", description = "Get all assessments for a class")
    public ResponseEntity<List<AssessmentResponse>> getClassAssessments(
            @PathVariable Long classId,
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /classes/{}/assessments - Request: subjectId={}, user={}",
                classId, subjectId, userDetails.getUsername());

        List<Assessment> assessments;
        if (subjectId != null) {
            assessments = assessmentRepository.findBySchoolClassIdAndSubjectIdOrderByDateDesc(classId, subjectId);
        } else {
            assessments = assessmentRepository.findBySchoolClassIdOrderByDateDesc(classId);
        }

        List<AssessmentResponse> response = assessments.stream()
                .map(a -> mapToAssessmentResponse(a, false))
                .collect(Collectors.toList());

        log.info("GET /classes/{}/assessments - Response: {} assessments found", classId, response.size());
        return ResponseEntity.ok(response);
    }

    private AssessmentResponse mapToAssessmentResponse(Assessment assessment, boolean includeScores) {
        List<AssessmentScore> scores = assessmentScoreRepository.findByAssessmentIdOrderByStudentUserLastNameAsc(assessment.getId());

        java.math.BigDecimal avgScore = null;
        java.math.BigDecimal avgPercentage = null;
        int scoredCount = (int) scores.stream().filter(s -> s.getScore() != null && !s.getAbsent()).count();

        if (scoredCount > 0) {
            avgScore = scores.stream()
                    .filter(s -> s.getScore() != null && !s.getAbsent())
                    .map(AssessmentScore::getScore)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    .divide(java.math.BigDecimal.valueOf(scoredCount), 2, java.math.RoundingMode.HALF_UP);

            avgPercentage = avgScore.multiply(java.math.BigDecimal.valueOf(100))
                    .divide(assessment.getMaxScore(), 2, java.math.RoundingMode.HALF_UP);
        }

        List<AssessmentResponse.ScoreInfo> scoreInfos = null;
        if (includeScores) {
            scoreInfos = scores.stream()
                    .map(s -> AssessmentResponse.ScoreInfo.builder()
                            .id(s.getId())
                            .studentId(s.getStudent().getId())
                            .studentName(s.getStudent().getUser().getFullName())
                            .studentNumber(s.getStudent().getStudentId())
                            .score(s.getScore())
                            .percentage(s.getPercentage())
                            .gradeLetter(s.getGradeLetter())
                            .remarks(s.getRemarks())
                            .absent(s.getAbsent())
                            .build())
                    .collect(Collectors.toList());
        }

        return AssessmentResponse.builder()
                .id(assessment.getId())
                .title(assessment.getTitle())
                .type(assessment.getType())
                .date(assessment.getDate())
                .maxScore(assessment.getMaxScore())
                .term(assessment.getTerm())
                .academicYear(assessment.getAcademicYear())
                .description(assessment.getDescription())
                .schoolClass(AssessmentResponse.ClassInfo.builder()
                        .id(assessment.getSchoolClass().getId())
                        .name(assessment.getSchoolClass().getName())
                        .grade(assessment.getSchoolClass().getGrade())
                        .build())
                .subject(AssessmentResponse.SubjectInfo.builder()
                        .id(assessment.getSubject().getId())
                        .name(assessment.getSubject().getName())
                        .code(assessment.getSubject().getCode())
                        .build())
                .teacher(AssessmentResponse.TeacherInfo.builder()
                        .id(assessment.getTeacher().getId())
                        .name(assessment.getTeacher().getUser().getFullName())
                        .build())
                .totalStudents(assessment.getSchoolClass().getStudents().size())
                .scoredStudents(scoredCount)
                .averageScore(avgScore)
                .averagePercentage(avgPercentage)
                .scores(scoreInfos)
                .build();
    }

    // Attendance endpoints
    @GetMapping("/classes/{classId}/attendance")
    @Operation(summary = "Get class attendance", description = "Get attendance for a class on a specific date")
    public ResponseEntity<List<AttendanceResponse>> getClassAttendance(
            @PathVariable Long classId,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /classes/{}/attendance - Request: classId={}, date={}, user={}",
                classId, classId, date, userDetails.getUsername());

        List<Attendance> attendanceList = attendanceRepository.findBySchoolClassIdAndDate(classId, date);

        // If no attendance records exist for this date, return empty list with all students
        if (attendanceList.isEmpty()) {
            List<com.eduflow.entity.academic.Student> students = studentRepository.findByCurrentClassId(classId);
            List<AttendanceResponse> response = students.stream()
                    .map(s -> AttendanceResponse.builder()
                            .studentId(s.getId())
                            .studentName(s.getUser().getFullName())
                            .studentNumber(s.getStudentId())
                            .classId(classId)
                            .date(date)
                            .status(null) // Not yet marked
                            .build())
                    .collect(Collectors.toList());
            log.info("GET /classes/{}/attendance - Response: No attendance marked, returning {} students",
                    classId, response.size());
            return ResponseEntity.ok(response);
        }

        List<AttendanceResponse> response = attendanceList.stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());

        log.info("GET /classes/{}/attendance - Response: {} attendance records found", classId, response.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classes/{classId}/attendance")
    @Operation(summary = "Mark attendance", description = "Mark or update attendance for students in a class")
    public ResponseEntity<List<AttendanceResponse>> markAttendance(
            @PathVariable Long classId,
            @RequestParam LocalDate date,
            @Valid @RequestBody List<MarkAttendanceRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /classes/{}/attendance - Request: classId={}, date={}, students={}, user={}",
                classId, classId, date, requests.size(), userDetails.getUsername());
        log.debug("POST /classes/{}/attendance - Request body: {}", classId, requests);

        Teacher teacher = getTeacherFromUser(userDetails);
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        List<AttendanceResponse> responses = new java.util.ArrayList<>();

        for (MarkAttendanceRequest request : requests) {
            com.eduflow.entity.academic.Student student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));

            Attendance attendance = attendanceRepository
                    .findByStudentIdAndSchoolClassIdAndDate(request.getStudentId(), classId, date)
                    .orElse(Attendance.builder()
                            .student(student)
                            .schoolClass(schoolClass)
                            .date(date)
                            .build());

            attendance.setStatus(request.getStatus());
            attendance.setRemarks(request.getRemarks());
            attendance.setMarkedBy(teacher);

            attendance = attendanceRepository.save(attendance);
            responses.add(mapToAttendanceResponse(attendance));
            log.debug("Marked attendance for student {} as {}", request.getStudentId(), request.getStatus());
        }

        log.info("POST /classes/{}/attendance - Response: {} attendance records saved", classId, responses.size());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/students/{studentId}/attendance")
    @Operation(summary = "Get student attendance", description = "Get attendance history for a student")
    public ResponseEntity<List<AttendanceResponse>> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /students/{}/attendance - Request: studentId={}, startDate={}, endDate={}, user={}",
                studentId, studentId, startDate, endDate, userDetails.getUsername());

        List<Attendance> attendanceList = attendanceRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);

        List<AttendanceResponse> response = attendanceList.stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());

        log.info("GET /students/{}/attendance - Response: {} attendance records found", studentId, response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/classes/{classId}/attendance/range")
    @Operation(summary = "Get class attendance for date range", description = "Get attendance records for a class within a date range")
    public ResponseEntity<ClassAttendanceRangeResponse> getClassAttendanceRange(
            @PathVariable Long classId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /classes/{}/attendance/range - Request: classId={}, startDate={}, endDate={}, user={}",
                classId, classId, startDate, endDate, userDetails.getUsername());

        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        List<Attendance> attendanceList = attendanceRepository.findBySchoolClassIdAndDateBetween(classId, startDate, endDate);

        List<ClassAttendanceRangeResponse.AttendanceRecord> records = attendanceList.stream()
                .map(a -> ClassAttendanceRangeResponse.AttendanceRecord.builder()
                        .studentId(a.getStudent().getId())
                        .studentName(a.getStudent().getUser().getFullName())
                        .studentNumber(a.getStudent().getStudentId())
                        .date(a.getDate())
                        .status(a.getStatus())
                        .remarks(a.getRemarks())
                        .build())
                .collect(Collectors.toList());

        ClassAttendanceRangeResponse response = ClassAttendanceRangeResponse.builder()
                .classId(classId)
                .className(schoolClass.getName())
                .startDate(startDate)
                .endDate(endDate)
                .totalRecords(records.size())
                .records(records)
                .build();

        log.info("GET /classes/{}/attendance/range - Response: {} records for class '{}'",
                classId, records.size(), schoolClass.getName());
        return ResponseEntity.ok(response);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ClassAttendanceRangeResponse {
        private Long classId;
        private String className;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalRecords;
        private List<AttendanceRecord> records;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class AttendanceRecord {
            private Long studentId;
            private String studentName;
            private String studentNumber;
            private LocalDate date;
            private Attendance.AttendanceStatus status;
            private String remarks;
        }
    }

    @PostMapping("/attendance/status")
    @Operation(summary = "Check attendance status", description = "Check if attendance is pending for given classes on current date")
    public ResponseEntity<List<AttendanceStatusResponse>> checkAttendanceStatus(
            @RequestBody List<Long> classIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /attendance/status - Request: classIds={}, user={}", classIds, userDetails.getUsername());

        LocalDate today = LocalDate.now();

        // Find which classes already have attendance marked today
        List<Long> classesWithAttendance = attendanceRepository.findClassIdsWithAttendanceOnDate(classIds, today);
        log.debug("Classes with attendance marked today: {}", classesWithAttendance);

        List<AttendanceStatusResponse> response = classIds.stream()
                .map(classId -> {
                    SchoolClass schoolClass = classRepository.findById(classId).orElse(null);
                    boolean hasAttendance = classesWithAttendance.contains(classId);
                    int studentCount = schoolClass != null ? schoolClass.getStudents().size() : 0;

                    return AttendanceStatusResponse.builder()
                            .classId(classId)
                            .className(schoolClass != null ? schoolClass.getName() : null)
                            .date(today)
                            .isPending(!hasAttendance)
                            .isCompleted(hasAttendance)
                            .totalStudents(studentCount)
                            .build();
                })
                .collect(Collectors.toList());

        long pendingCount = response.stream().filter(AttendanceStatusResponse::isPending).count();
        long completedCount = response.stream().filter(AttendanceStatusResponse::isCompleted).count();
        log.info("POST /attendance/status - Response: {} classes checked, {} pending, {} completed",
                response.size(), pendingCount, completedCount);

        return ResponseEntity.ok(response);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AttendanceStatusResponse {
        private Long classId;
        private String className;
        private LocalDate date;
        private boolean isPending;
        private boolean isCompleted;
        private int totalStudents;
    }

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getUser().getFullName())
                .studentNumber(attendance.getStudent().getStudentId())
                .classId(attendance.getSchoolClass().getId())
                .className(attendance.getSchoolClass().getName())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .markedByName(attendance.getMarkedBy() != null ? attendance.getMarkedBy().getUser().getFullName() : null)
                .build();
    }

    @lombok.Data
    public static class MarkAttendanceRequest {
        @jakarta.validation.constraints.NotNull
        private Long studentId;
        @jakarta.validation.constraints.NotNull
        private Attendance.AttendanceStatus status;
        private String remarks;
    }

    // Announcement endpoints
    @GetMapping("/announcements")
    @Operation(summary = "Get announcements", description = "Get all announcements for teachers")
    public ResponseEntity<PagedResponse<AnnouncementResponse>> getAnnouncements(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Teacher teacher = getTeacherFromUser(userDetails);
        Long userId = teacher.getUser().getId();

        Page<Announcement> page = announcementRepository.findAnnouncementsForTeacher(userId, pageable);

        List<Long> readIds = announcementReadRepository.findReadAnnouncementIdsByUserId(userId);

        List<AnnouncementResponse> content = page.getContent().stream()
                .map(a -> mapToAnnouncementResponse(a, readIds.contains(a.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/announcements/unread-count")
    @Operation(summary = "Get unread announcements count", description = "Get count of unread announcements")
    public ResponseEntity<Long> getUnreadAnnouncementCount(@AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);
        Long userId = teacher.getUser().getId();
        return ResponseEntity.ok(announcementReadRepository.countUnreadForTeacher(userId));
    }

    @PostMapping("/announcements/{id}/read")
    @Operation(summary = "Mark announcement as read", description = "Mark a specific announcement as read")
    public ResponseEntity<MessageResponse> markAnnouncementAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Teacher teacher = getTeacherFromUser(userDetails);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!announcementReadRepository.existsByAnnouncementIdAndUserId(id, teacher.getUser().getId())) {
            AnnouncementRead read = AnnouncementRead.builder()
                    .announcement(announcement)
                    .user(teacher.getUser())
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

    private Teacher getTeacherFromUser(UserDetails userDetails) {
        // In a real implementation, this would look up the teacher by user email
        return teacherRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals(userDetails.getUsername()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));
    }

    private String calculateGradeLetter(java.math.BigDecimal score, java.math.BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "N/A";
        }

        double percentage = score.doubleValue() / maxScore.doubleValue() * 100;

        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        if (percentage >= 50) return "E";
        return "F";
    }

    private ClassResponse mapToClassResponse(SchoolClass sc) {
        ClassResponse.TeacherSummary teacherSummary = null;
        if (sc.getClassTeacher() != null) {
            teacherSummary = ClassResponse.TeacherSummary.builder()
                    .id(sc.getClassTeacher().getId())
                    .employeeId(sc.getClassTeacher().getEmployeeId())
                    .name(sc.getClassTeacher().getUser().getFullName())
                    .email(sc.getClassTeacher().getUser().getEmail())
                    .build();
        }

        List<ClassResponse.SubjectSummary> subjects = sc.getSubjects().stream()
                .map(s -> ClassResponse.SubjectSummary.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .code(s.getCode())
                        .build())
                .collect(Collectors.toList());

        return ClassResponse.builder()
                .id(sc.getId())
                .name(sc.getName())
                .grade(sc.getGrade())
                .academicYear(sc.getAcademicYear())
                .section(sc.getSection())
                .maxCapacity(sc.getMaxCapacity())
                .currentEnrollment(sc.getStudents().size())
                .classTeacher(teacherSummary)
                .subjects(subjects)
                .build();
    }

    private StudentResponse mapToStudentResponse(com.eduflow.entity.academic.Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .email(student.getUser().getEmail())
                .firstName(student.getUser().getFirstName())
                .lastName(student.getUser().getLastName())
                .fullName(student.getUser().getFullName())
                .phone(student.getUser().getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .status(student.getStatus())
                .build();
    }

    private GradeResponse mapToGradeResponse(Grade grade) {
        return GradeResponse.builder()
                .id(grade.getId())
                .score(grade.getScore())
                .maxScore(grade.getMaxScore())
                .percentage(grade.getPercentage())
                .gradeLetter(grade.getGradeLetter())
                .term(grade.getTerm())
                .academicYear(grade.getAcademicYear())
                .teacherComment(grade.getTeacherComment())
                .subject(GradeResponse.SubjectInfo.builder()
                        .id(grade.getSubject().getId())
                        .name(grade.getSubject().getName())
                        .code(grade.getSubject().getCode())
                        .build())
                .student(GradeResponse.StudentInfo.builder()
                        .id(grade.getEnrollment().getStudent().getId())
                        .studentId(grade.getEnrollment().getStudent().getStudentId())
                        .name(grade.getEnrollment().getStudent().getUser().getFullName())
                        .build())
                .build();
    }

    private HomeworkResponse mapToHomeworkResponse(Homework homework) {
        return HomeworkResponse.builder()
                .id(homework.getId())
                .title(homework.getTitle())
                .description(homework.getDescription())
                .dueDate(homework.getDueDate())
                .attachments(homework.getAttachments())
                .maxScore(homework.getMaxScore())
                .status(homework.getStatus())
                .academicYear(homework.getAcademicYear())
                .term(homework.getTerm())
                .overdue(homework.isOverdue())
                .subject(HomeworkResponse.SubjectInfo.builder()
                        .id(homework.getSubject().getId())
                        .name(homework.getSubject().getName())
                        .code(homework.getSubject().getCode())
                        .build())
                .schoolClass(HomeworkResponse.ClassInfo.builder()
                        .id(homework.getSchoolClass().getId())
                        .name(homework.getSchoolClass().getName())
                        .grade(homework.getSchoolClass().getGrade())
                        .build())
                .teacher(HomeworkResponse.TeacherInfo.builder()
                        .id(homework.getTeacher().getId())
                        .name(homework.getTeacher().getUser().getFullName())
                        .build())
                .build();
    }
}
