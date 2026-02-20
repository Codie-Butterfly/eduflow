package com.eduflow.service;

import com.eduflow.dto.request.CreateTeacherRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.TeacherResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeacherService {

    TeacherResponse createTeacher(CreateTeacherRequest request);

    TeacherResponse getTeacherById(Long id);

    TeacherResponse getTeacherByEmployeeId(String employeeId);

    PagedResponse<TeacherResponse> getAllTeachers(Pageable pageable);

    List<TeacherResponse> searchTeachers(String name);

    TeacherResponse updateTeacher(Long id, CreateTeacherRequest request);

    void deleteTeacher(Long id);

    TeacherResponse addSubjectToTeacher(Long teacherId, Long subjectId);

    TeacherResponse removeSubjectFromTeacher(Long teacherId, Long subjectId);
}
