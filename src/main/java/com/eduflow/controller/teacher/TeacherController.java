package com.eduflow.controller.teacher;

import com.eduflow.dto.request.CreateGradeRequest;
import com.eduflow.dto.request.CreateHomeworkRequest;
import com.eduflow.dto.response.*;
import com.eduflow.entity.academic.Grade;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.entity.communication.Homework;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.*;
import com.eduflow.repository.communication.HomeworkRepository;
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
import java.util.stream.Collectors;

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
