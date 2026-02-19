package com.eduflow.controller.admin;

import com.eduflow.dto.request.CreateSubjectRequest;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.SubjectResponse;
import com.eduflow.service.SubjectService;
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
@RequestMapping("/v1/admin/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Subjects", description = "Subject management endpoints")
public class AdminSubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "List all subjects", description = "Get paginated list of all subjects")
    public ResponseEntity<PagedResponse<SubjectResponse>> getAllSubjects(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(subjectService.getAllSubjects(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID", description = "Get subject details by ID")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get subject by code", description = "Get subject by unique code")
    public ResponseEntity<SubjectResponse> getSubjectByCode(@PathVariable String code) {
        return ResponseEntity.ok(subjectService.getSubjectByCode(code));
    }

    @PostMapping
    @Operation(summary = "Create subject", description = "Create a new subject")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.ok(subjectService.createSubject(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subject", description = "Update an existing subject")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subject", description = "Delete a subject")
    public ResponseEntity<MessageResponse> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(MessageResponse.success("Subject deleted successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search subjects", description = "Search subjects by name")
    public ResponseEntity<List<SubjectResponse>> searchSubjects(@RequestParam String name) {
        return ResponseEntity.ok(subjectService.searchSubjects(name));
    }

    @GetMapping("/mandatory")
    @Operation(summary = "Get mandatory subjects", description = "Get all mandatory subjects")
    public ResponseEntity<List<SubjectResponse>> getMandatorySubjects() {
        return ResponseEntity.ok(subjectService.getMandatorySubjects());
    }
}
