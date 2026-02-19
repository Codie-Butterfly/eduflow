package com.eduflow.repository.report;

import com.eduflow.entity.report.StudentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentReportRepository extends JpaRepository<StudentReport, Long> {

    List<StudentReport> findByStudentId(Long studentId);

    List<StudentReport> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    Optional<StudentReport> findByStudentIdAndTermAndAcademicYear(
            Long studentId, StudentReport.Term term, String academicYear);

    @Query("SELECT sr FROM StudentReport sr WHERE sr.enrollment.schoolClass.id = :classId " +
            "AND sr.term = :term AND sr.academicYear = :academicYear")
    List<StudentReport> findByClassAndTermAndYear(
            @Param("classId") Long classId,
            @Param("term") StudentReport.Term term,
            @Param("academicYear") String academicYear);

    @Query("SELECT sr FROM StudentReport sr WHERE sr.status = :status")
    List<StudentReport> findByStatus(@Param("status") StudentReport.ReportStatus status);

    @Query("SELECT sr FROM StudentReport sr WHERE sr.student.id = :studentId AND sr.status = 'PUBLISHED' " +
            "ORDER BY sr.academicYear DESC, sr.term DESC")
    List<StudentReport> findPublishedReportsByStudentId(@Param("studentId") Long studentId);

    boolean existsByStudentIdAndTermAndAcademicYear(Long studentId, StudentReport.Term term, String academicYear);
}
