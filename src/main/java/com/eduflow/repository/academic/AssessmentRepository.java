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
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    // Find assessment by ID with eager loading
    @Query("SELECT a FROM Assessment a " +
            "LEFT JOIN FETCH a.schoolClass " +
            "LEFT JOIN FETCH a.subject " +
            "LEFT JOIN FETCH a.teacher t " +
            "LEFT JOIN FETCH t.user " +
            "WHERE a.id = :id")
    Optional<Assessment> findByIdWithDetails(@Param("id") Long id);

    // Find assessments by teacher with eager loading
    @Query("SELECT a FROM Assessment a " +
            "LEFT JOIN FETCH a.schoolClass " +
            "LEFT JOIN FETCH a.subject " +
            "LEFT JOIN FETCH a.teacher t " +
            "LEFT JOIN FETCH t.user " +
            "WHERE a.teacher.id = :teacherId " +
            "ORDER BY a.date DESC")
    List<Assessment> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    // Find assessments by teacher and academic year with eager loading
    @Query("SELECT a FROM Assessment a " +
            "LEFT JOIN FETCH a.schoolClass " +
            "LEFT JOIN FETCH a.subject " +
            "LEFT JOIN FETCH a.teacher t " +
            "LEFT JOIN FETCH t.user " +
            "WHERE a.teacher.id = :teacherId AND a.academicYear = :academicYear " +
            "ORDER BY a.date DESC")
    List<Assessment> findByTeacherIdAndAcademicYearWithDetails(@Param("teacherId") Long teacherId,
                                                                @Param("academicYear") String academicYear);

    // Find assessments by class with eager loading
    @Query("SELECT a FROM Assessment a " +
            "LEFT JOIN FETCH a.schoolClass " +
            "LEFT JOIN FETCH a.subject " +
            "LEFT JOIN FETCH a.teacher t " +
            "LEFT JOIN FETCH t.user " +
            "WHERE a.schoolClass.id = :classId " +
            "ORDER BY a.date DESC")
    List<Assessment> findBySchoolClassIdWithDetails(@Param("classId") Long classId);

    // Find assessments by class and subject with eager loading
    @Query("SELECT a FROM Assessment a " +
            "LEFT JOIN FETCH a.schoolClass " +
            "LEFT JOIN FETCH a.subject " +
            "LEFT JOIN FETCH a.teacher t " +
            "LEFT JOIN FETCH t.user " +
            "WHERE a.schoolClass.id = :classId AND a.subject.id = :subjectId " +
            "ORDER BY a.date DESC")
    List<Assessment> findBySchoolClassIdAndSubjectIdWithDetails(@Param("classId") Long classId,
                                                                 @Param("subjectId") Long subjectId);

    // Original methods (kept for backward compatibility)
    List<Assessment> findByTeacherIdOrderByDateDesc(Long teacherId);

    Page<Assessment> findByTeacherIdOrderByDateDesc(Long teacherId, Pageable pageable);

    List<Assessment> findBySchoolClassIdOrderByDateDesc(Long classId);

    List<Assessment> findBySchoolClassIdAndSubjectIdOrderByDateDesc(Long classId, Long subjectId);

    List<Assessment> findBySchoolClassIdAndSubjectIdAndTermOrderByDateDesc(
            Long classId, Long subjectId, Grade.Term term);

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