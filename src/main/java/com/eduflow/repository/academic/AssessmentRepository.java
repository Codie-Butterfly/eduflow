package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Assessment;
import com.eduflow.entity.academic.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    // Find assessments by teacher
    List<Assessment> findByTeacherIdOrderByDateDesc(Long teacherId);

    Page<Assessment> findByTeacherIdOrderByDateDesc(Long teacherId, Pageable pageable);

    // Find assessments by class
    List<Assessment> findBySchoolClassIdOrderByDateDesc(Long classId);

    // Find assessments by class and subject
    List<Assessment> findBySchoolClassIdAndSubjectIdOrderByDateDesc(Long classId, Long subjectId);

    // Find assessments by class, subject and term
    List<Assessment> findBySchoolClassIdAndSubjectIdAndTermOrderByDateDesc(
            Long classId, Long subjectId, Grade.Term term);

    // Find assessments by teacher for a specific academic year
    List<Assessment> findByTeacherIdAndAcademicYearOrderByDateDesc(Long teacherId, String academicYear);

    // Find assessments by class and date range
    List<Assessment> findBySchoolClassIdAndDateBetweenOrderByDateDesc(
            Long classId, LocalDate startDate, LocalDate endDate);

    // Find assessments by teacher, class and subject
    @Query("SELECT a FROM Assessment a WHERE a.teacher.id = :teacherId " +
            "AND a.schoolClass.id = :classId AND a.subject.id = :subjectId " +
            "ORDER BY a.date DESC")
    List<Assessment> findByTeacherClassAndSubject(@Param("teacherId") Long teacherId,
                                                   @Param("classId") Long classId,
                                                   @Param("subjectId") Long subjectId);
}