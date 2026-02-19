package com.eduflow.service.impl;

import com.eduflow.dto.request.CreateSubjectRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.SubjectResponse;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Subject;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.exception.BadRequestException;
import com.eduflow.exception.DuplicateResourceException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.SubjectRepository;
import com.eduflow.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        if (subjectRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Subject", "code", request.getCode());
        }

        Subject subject = Subject.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .creditHours(request.getCreditHours())
                .mandatory(request.isMandatory())
                .build();

        subject = subjectRepository.save(subject);
        log.info("Subject created: {} ({})", subject.getName(), subject.getCode());

        return mapToResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
        return mapToResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectByCode(String code) {
        Subject subject = subjectRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "code", code));
        return mapToResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SubjectResponse> getAllSubjects(Pageable pageable) {
        Page<Subject> page = subjectRepository.findAll(pageable);
        List<SubjectResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> searchSubjects(String name) {
        return subjectRepository.searchByName(name).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getMandatorySubjects() {
        return subjectRepository.findByMandatory(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long id, CreateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));

        // Check for duplicate code if changed
        if (!subject.getCode().equalsIgnoreCase(request.getCode())) {
            if (subjectRepository.existsByCode(request.getCode().toUpperCase())) {
                throw new DuplicateResourceException("Subject", "code", request.getCode());
            }
        }

        subject.setName(request.getName());
        subject.setCode(request.getCode().toUpperCase());
        subject.setDescription(request.getDescription());
        subject.setCreditHours(request.getCreditHours());
        subject.setMandatory(request.isMandatory());

        subject = subjectRepository.save(subject);
        log.info("Subject updated: {}", subject.getId());

        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));

        if (!subject.getClasses().isEmpty()) {
            throw new BadRequestException("Cannot delete subject assigned to " + subject.getClasses().size() + " classes");
        }

        if (!subject.getTeachers().isEmpty()) {
            throw new BadRequestException("Cannot delete subject assigned to " + subject.getTeachers().size() + " teachers");
        }

        subjectRepository.delete(subject);
        log.info("Subject deleted: {}", id);
    }

    private SubjectResponse mapToResponse(Subject subject) {
        List<SubjectResponse.ClassSummary> classSummaries = subject.getClasses().stream()
                .map(c -> SubjectResponse.ClassSummary.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .grade(c.getGrade())
                        .academicYear(c.getAcademicYear())
                        .build())
                .collect(Collectors.toList());

        List<SubjectResponse.TeacherSummary> teacherSummaries = subject.getTeachers().stream()
                .map(t -> SubjectResponse.TeacherSummary.builder()
                        .id(t.getId())
                        .name(t.getUser().getFullName())
                        .employeeId(t.getEmployeeId())
                        .build())
                .collect(Collectors.toList());

        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .creditHours(subject.getCreditHours())
                .mandatory(subject.isMandatory())
                .classes(classSummaries)
                .teachers(teacherSummaries)
                .build();
    }
}
