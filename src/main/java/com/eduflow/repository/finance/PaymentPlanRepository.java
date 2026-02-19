package com.eduflow.repository.finance;

import com.eduflow.entity.finance.PaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {

    List<PaymentPlan> findByStudentId(Long studentId);

    List<PaymentPlan> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.student.id = :studentId AND pp.status = :status")
    List<PaymentPlan> findByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") PaymentPlan.PlanStatus status);

    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = :status")
    List<PaymentPlan> findByStatus(@Param("status") PaymentPlan.PlanStatus status);

    Optional<PaymentPlan> findByStudentIdAndAcademicYearAndStatus(
            Long studentId, String academicYear, PaymentPlan.PlanStatus status);
}
