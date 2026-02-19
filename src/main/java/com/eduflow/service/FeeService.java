package com.eduflow.service;

import com.eduflow.dto.request.AssignFeeRequest;
import com.eduflow.dto.request.CreateFeeRequest;
import com.eduflow.dto.response.FeeResponse;
import com.eduflow.dto.response.StudentFeeResponse;

import java.util.List;

public interface FeeService {

    FeeResponse createFee(CreateFeeRequest request);

    FeeResponse getFeeById(Long id);

    List<FeeResponse> getAllFees();

    List<FeeResponse> getFeesByAcademicYear(String academicYear);

    FeeResponse updateFee(Long id, CreateFeeRequest request);

    void deleteFee(Long id);

    List<StudentFeeResponse> assignFeesToStudents(AssignFeeRequest request);

    List<StudentFeeResponse> getStudentFees(Long studentId);

    List<StudentFeeResponse> getStudentFeesByYear(Long studentId, String academicYear);

    StudentFeeResponse applyDiscount(Long assignmentId, java.math.BigDecimal discountAmount, String reason);

    StudentFeeResponse waiveFee(Long assignmentId, String reason);
}
