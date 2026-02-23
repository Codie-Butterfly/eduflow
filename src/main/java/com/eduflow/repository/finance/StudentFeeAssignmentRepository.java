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

    @Query("SELECT SUM(sfa.amountPaid) FROM StudentFeeAssignment sfa " +
            "WHERE sfa.student.id = :studentId AND sfa.academicYear = :academicYear")
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

    @Query("SELECT COALESCE(SUM(sfa.amount - sfa.discountAmount - sfa.amountPaid), 0) FROM StudentFeeAssignment sfa " +
            "WHERE sfa.status NOT IN ('PAID', 'WAIVED')")
    BigDecimal calculateTotalOutstandingFees();
}
