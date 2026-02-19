package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByEnrollmentId(Long enrollmentId);

    List<Grade> findByEnrollmentIdAndTerm(Long enrollmentId, Grade.Term term);

    List<Grade> findBySubjectIdAndAcademicYear(Long subjectId, String academicYear);

    Optional<Grade> findByEnrollmentIdAndSubjectIdAndTerm(Long enrollmentId, Long subjectId, Grade.Term term);

    @Query("SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND g.academicYear = :academicYear")
    List<Grade> findByStudentIdAndAcademicYear(
            @Param("studentId") Long studentId,
            @Param("academicYear") String academicYear);

    @Query("SELECT g FROM Grade g WHERE g.enrollment.schoolClass.id = :classId AND g.subject.id = :subjectId " +
            "AND g.term = :term AND g.academicYear = :academicYear")
    List<Grade> findByClassAndSubjectAndTerm(
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId,
            @Param("term") Grade.Term term,
            @Param("academicYear") String academicYear);

    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.enrollment.schoolClass.id = :classId " +
            "AND g.subject.id = :subjectId AND g.academicYear = :academicYear")
    Double calculateAverageByClassAndSubject(
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId,
            @Param("academicYear") String academicYear);

    @Query("SELECT g FROM Grade g WHERE g.gradedBy.id = :teacherId AND g.academicYear = :academicYear")
    List<Grade> findByTeacherIdAndAcademicYear(
            @Param("teacherId") Long teacherId,
            @Param("academicYear") String academicYear);
}
