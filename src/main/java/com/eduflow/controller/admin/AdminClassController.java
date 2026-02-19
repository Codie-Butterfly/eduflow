package com.eduflow.controller.admin;

import com.eduflow.dto.request.CreateClassRequest;
import com.eduflow.dto.response.ClassResponse;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.StudentResponse;
import com.eduflow.service.ClassService;
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
@RequestMapping("/v1/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Classes", description = "Class management endpoints")
public class AdminClassController {

    private final ClassService classService;

    @GetMapping
    @Operation(summary = "List all classes", description = "Get paginated list of all classes with optional filters")
    public ResponseEntity<PagedResponse<ClassResponse>> getAllClasses(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer grade,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(classService.getClassesByFilters(academicYear, grade, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get class by ID", description = "Get class details by ID")
    public ResponseEntity<ClassResponse> getClassById(@PathVariable Long id) {
        return ResponseEntity.ok(classService.getClassById(id));
    }

    @PostMapping
    @Operation(summary = "Create class", description = "Create a new class")
    public ResponseEntity<ClassResponse> createClass(@Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(classService.createClass(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update class", description = "Update an existing class")
    public ResponseEntity<ClassResponse> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(classService.updateClass(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete class", description = "Deactivate a class")
    public ResponseEntity<MessageResponse> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.ok(MessageResponse.success("Class deactivated successfully"));
    }

    @GetMapping("/{id}/students")
    @Operation(summary = "Get class students", description = "Get all students in a specific class")
    public ResponseEntity<List<StudentResponse>> getClassStudents(@PathVariable Long id) {
        return ResponseEntity.ok(classService.getClassStudents(id));
    }

    @PostMapping("/{id}/students/{studentId}")
    @Operation(summary = "Assign student to class", description = "Assign a student to this class")
    public ResponseEntity<StudentResponse> assignStudentToClass(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(classService.assignStudentToClass(id, studentId));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @Operation(summary = "Remove student from class", description = "Remove a student from this class")
    public ResponseEntity<MessageResponse> removeStudentFromClass(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        classService.removeStudentFromClass(id, studentId);
        return ResponseEntity.ok(MessageResponse.success("Student removed from class successfully"));
    }

    @PostMapping("/{id}/subjects/{subjectId}")
    @Operation(summary = "Add subject to class", description = "Add a subject to this class")
    public ResponseEntity<ClassResponse> addSubjectToClass(
            @PathVariable Long id,
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(classService.addSubjectToClass(id, subjectId));
    }

    @DeleteMapping("/{id}/subjects/{subjectId}")
    @Operation(summary = "Remove subject from class", description = "Remove a subject from this class")
    public ResponseEntity<ClassResponse> removeSubjectFromClass(
            @PathVariable Long id,
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(classService.removeSubjectFromClass(id, subjectId));
    }
}
