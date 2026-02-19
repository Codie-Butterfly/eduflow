package com.eduflow.service;

import com.eduflow.dto.request.CreateStudentRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.StudentResponse;
import com.eduflow.entity.academic.Student;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    StudentResponse createStudent(CreateStudentRequest request);

    StudentResponse getStudentById(Long id);

    StudentResponse getStudentByStudentId(String studentId);

    PagedResponse<StudentResponse> getAllStudents(Pageable pageable);

    List<StudentResponse> getStudentsByClassId(Long classId);

    List<StudentResponse> getStudentsByParentId(Long parentId);

    StudentResponse updateStudent(Long id, CreateStudentRequest request);

    void deleteStudent(Long id);

    StudentResponse enrollStudentInClass(Long studentId, Long classId);

    PagedResponse<StudentResponse> searchStudents(String name, Pageable pageable);

    PagedResponse<StudentResponse> getStudentsByStatus(Student.StudentStatus status, Pageable pageable);
}
