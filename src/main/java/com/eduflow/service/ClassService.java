package com.eduflow.service;

import com.eduflow.dto.request.CreateClassRequest;
import com.eduflow.dto.response.ClassResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.StudentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClassService {

    ClassResponse createClass(CreateClassRequest request);

    ClassResponse getClassById(Long id);

    PagedResponse<ClassResponse> getAllClasses(Pageable pageable);

    PagedResponse<ClassResponse> getClassesByFilters(String academicYear, Integer grade, Pageable pageable);

    ClassResponse updateClass(Long id, CreateClassRequest request);

    void deleteClass(Long id);

    List<StudentResponse> getClassStudents(Long classId);

    StudentResponse assignStudentToClass(Long classId, Long studentId);

    void removeStudentFromClass(Long classId, Long studentId);

    ClassResponse addSubjectToClass(Long classId, Long subjectId);

    ClassResponse removeSubjectFromClass(Long classId, Long subjectId);
}