package com.eduflow.repository.academic;

import com.eduflow.entity.academic.AssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, Long> {

    // Find all scores for an assessment
    List<AssessmentScore> findByAssessmentIdOrderByStudentUserLastNameAsc(Long assessmentId);

    // Find score for specific student and assessment
    Optional<AssessmentScore> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);

    // Find all scores for a student
    List<AssessmentScore> findByStudentIdOrderByAssessmentDateDesc(Long studentId);

    // Find all scores for a student in a specific class
    @Query("SELECT s FROM AssessmentScore s WHERE s.student.id = :studentId " +
            "AND s.assessment.schoolClass.id = :classId ORDER BY s.assessment.date DESC")
    List<AssessmentScore> findByStudentAndClass(@Param("studentId") Long studentId,
                                                 @Param("classId") Long classId);

    // Find all scores for a student in a specific subject
    @Query("SELECT s FROM AssessmentScore s WHERE s.student.id = :studentId " +
            "AND s.assessment.subject.id = :subjectId ORDER BY s.assessment.date DESC")
    List<AssessmentScore> findByStudentAndSubject(@Param("studentId") Long studentId,
                                                   @Param("subjectId") Long subjectId);

    // Calculate average score for a student in a subject
    @Query("SELECT AVG(s.score * 100 / s.assessment.maxScore) FROM AssessmentScore s " +
            "WHERE s.student.id = :studentId AND s.assessment.subject.id = :subjectId " +
            "AND s.absent = false AND s.score IS NOT NULL")
    BigDecimal calculateAveragePercentage(@Param("studentId") Long studentId,
                                          @Param("subjectId") Long subjectId);

    // Count scores for an assessment
    long countByAssessmentId(Long assessmentId);

    // Find scores for a student within a date range with eager loading
    @Query("SELECT s FROM AssessmentScore s " +
            "LEFT JOIN FETCH s.assessment a " +
            "LEFT JOIN FETCH a.subject " +
            "WHERE s.student.id = :studentId " +
            "AND a.date BETWEEN :startDate AND :endDate " +
            "ORDER BY a.date DESC")
    List<AssessmentScore> findByStudentIdAndDateRange(@Param("studentId") Long studentId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    // Find all scores for a student with eager loading
    @Query("SELECT s FROM AssessmentScore s " +
            "LEFT JOIN FETCH s.assessment a " +
            "LEFT JOIN FETCH a.subject " +
            "WHERE s.student.id = :studentId " +
            "ORDER BY a.date DESC")
    List<AssessmentScore> findByStudentIdWithDetails(@Param("studentId") Long studentId);
}