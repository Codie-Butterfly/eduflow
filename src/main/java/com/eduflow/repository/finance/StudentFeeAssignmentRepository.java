package com.eduflow.repository.finance;

import com.eduflow.entity.finance.StudentFeeAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFeeAssignmentRepository extends JpaRepository<StudentFeeAssignment, Long> {

    List<StudentFeeAssignment> findByStudentId(Long studentId);

    List<StudentFeeAssignment> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    List<StudentFeeAssignment> findByFeeId(Long feeId);

    Optional<StudentFeeAssignment> findByStudentIdAndFeeIdAndAcademicYear(Long studentId, Long feeId, String academicYear);

    @Query("SELECT sfa FROM StudentFeeAssignment sfa WHERE sfa.student.id = :studentId AND sfa.status = :status")
    List<StudentFeeAssignment> findByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") StudentFeeAssignment.FeeStatus status);

    @Query("SELECT sfa FROM StudentFeeAssignment sfa WHERE sfa.status = :status")
    Page<StudentFeeAssignment> findByStatus(@Param("status") StudentFeeAssignment.FeeStatus status, Pageable pageable);

    @Query("SELECT SUM(sfa.amount - sfa.discountAmount) FROM StudentFeeAssignment sfa " +
            "WHERE sfa.student.id = :studentId AND sfa.academicYear = :academicYear")
    BigDecimal calculateTotalFeesByStudentAndYear(
            @Param("studentId") Long studentId,
            @Param("academicYear") String academicYear);

    @Query(value = "SELECT COALESCE(SUM(p.amount), 0) FROM payments p " +
            "JOIN student_fee_assignments sfa ON p.student_fee_assignment_id = sfa.id " +
            "WHERE p.status = 'COMPLETED' AND sfa.student_id = :studentId AND sfa.academic_year = :academicYear",
            nativeQuery = true)
    BigDecimal calculateTotalPaidByStudentAndYear(
            @Param("studentId") Long studentId,
            @Param("academicYear") String academicYear);

    @Query("SELECT sfa FROM StudentFeeAssignment sfa WHERE sfa.dueDate < CURRENT_DATE " +
            "AND sfa.status NOT IN ('PAID', 'WAIVED')")
    List<StudentFeeAssignment> findOverdueFees();

    @Query("SELECT sfa FROM StudentFeeAssignment sfa " +
            "WHERE sfa.student.currentClass.id = :classId AND sfa.academicYear = :academicYear")
    List<StudentFeeAssignment> findByClassIdAndAcademicYear(
            @Param("classId") Long classId,
            @Param("academicYear") String academicYear);

    @Query(value = "SELECT COALESCE(" +
            "(SELECT SUM(sfa.amount - sfa.discount_amount) FROM student_fee_assignments sfa WHERE sfa.status NOT IN ('PAID', 'WAIVED')) - " +
            "(SELECT COALESCE(SUM(p.amount), 0) FROM payments p " +
            "JOIN student_fee_assignments sfa ON p.student_fee_assignment_id = sfa.id " +
            "WHERE p.status = 'COMPLETED' AND sfa.status NOT IN ('PAID', 'WAIVED')), 0)",
            nativeQuery = true)
    BigDecimal calculateTotalOutstandingFees();
}
