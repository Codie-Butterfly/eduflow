package com.eduflow.controller.admin;

import com.eduflow.dto.request.CreateTeacherRequest;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.TeacherResponse;
import com.eduflow.service.TeacherService;
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
@RequestMapping("/v1/admin/staff/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Teachers", description = "Teacher management endpoints")
public class AdminTeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @Operation(summary = "List all teachers", description = "Get paginated list of all teachers")
    public ResponseEntity<PagedResponse<TeacherResponse>> getAllTeachers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(teacherService.getAllTeachers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get teacher by ID", description = "Get teacher details by ID")
    public ResponseEntity<TeacherResponse> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @GetMapping("/employee-id/{employeeId}")
    @Operation(summary = "Get teacher by employee ID", description = "Get teacher by employee ID")
    public ResponseEntity<TeacherResponse> getTeacherByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(teacherService.getTeacherByEmployeeId(employeeId));
    }

    @PostMapping
    @Operation(summary = "Create teacher", description = "Create a new teacher")
    public ResponseEntity<TeacherResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.createTeacher(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update teacher", description = "Update an existing teacher")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete teacher", description = "Deactivate a teacher")
    public ResponseEntity<MessageResponse> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok(MessageResponse.success("Teacher deactivated successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search teachers", description = "Search teachers by name")
    public ResponseEntity<List<TeacherResponse>> searchTeachers(@RequestParam String name) {
        return ResponseEntity.ok(teacherService.searchTeachers(name));
    }

    @PostMapping("/{id}/subjects/{subjectId}")
    @Operation(summary = "Add subject to teacher", description = "Assign a subject to a teacher")
    public ResponseEntity<TeacherResponse> addSubjectToTeacher(
            @PathVariable Long id,
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(teacherService.addSubjectToTeacher(id, subjectId));
    }

    @DeleteMapping("/{id}/subjects/{subjectId}")
    @Operation(summary = "Remove subject from teacher", description = "Remove a subject from a teacher")
    public ResponseEntity<TeacherResponse> removeSubjectFromTeacher(
            @PathVariable Long id,
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(teacherService.removeSubjectFromTeacher(id, subjectId));
    }
}
