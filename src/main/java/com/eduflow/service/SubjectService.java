package com.eduflow.service;

import com.eduflow.dto.request.CreateSubjectRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.SubjectResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubjectService {

    SubjectResponse createSubject(CreateSubjectRequest request);

    SubjectResponse getSubjectById(Long id);

    SubjectResponse getSubjectByCode(String code);

    PagedResponse<SubjectResponse> getAllSubjects(Pageable pageable);

    List<SubjectResponse> searchSubjects(String name);

    List<SubjectResponse> getMandatorySubjects();

    SubjectResponse updateSubject(Long id, CreateSubjectRequest request);

    void deleteSubject(Long id);
}
