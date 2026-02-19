package com.eduflow.controller.admin;

import com.eduflow.dto.request.CreateStudentRequest;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.StudentResponse;
import com.eduflow.entity.academic.Student;
import com.eduflow.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Students", description = "Student management endpoints")
public class AdminStudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "List all students", description = "Get paginated list of all students")
    public ResponseEntity<PagedResponse<StudentResponse>> getAllStudents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentService.getAllStudents(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student by ID", description = "Get student details by ID")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/student-id/{studentId}")
    @Operation(summary = "Get student by student ID", description = "Get student by their unique student ID")
    public ResponseEntity<StudentResponse> getStudentByStudentId(@PathVariable String studentId) {
        return ResponseEntity.ok(studentService.getStudentByStudentId(studentId));
    }

    @PostMapping
    @Operation(summary = "Create student", description = "Create a new student")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.ok(studentService.createStudent(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update student", description = "Update an existing student")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete student", description = "Deactivate a student")
    public ResponseEntity<MessageResponse> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(MessageResponse.success("Student deactivated successfully"));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get students by class", description = "Get all students in a specific class")
    public ResponseEntity<List<StudentResponse>> getStudentsByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(studentService.getStudentsByClassId(classId));
    }

    @PostMapping("/{id}/enroll")
    @Operation(summary = "Enroll student in class", description = "Enroll a student in a class")
    public ResponseEntity<StudentResponse> enrollStudentInClass(
            @PathVariable Long id,
            @RequestParam Long classId) {
        return ResponseEntity.ok(studentService.enrollStudentInClass(id, classId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search students", description = "Search students by name")
    public ResponseEntity<PagedResponse<StudentResponse>> searchStudents(
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentService.searchStudents(name, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get students by status", description = "Get students filtered by status")
    public ResponseEntity<PagedResponse<StudentResponse>> getStudentsByStatus(
            @PathVariable Student.StudentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentService.getStudentsByStatus(status, pageable));
    }
}
