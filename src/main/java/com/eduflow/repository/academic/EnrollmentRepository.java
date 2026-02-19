package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findBySchoolClassId(Long classId);

    List<Enrollment> findByAcademicYear(String academicYear);

    Optional<Enrollment> findByStudentIdAndSchoolClassIdAndAcademicYear(
            Long studentId, Long classId, String academicYear);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status")
    List<Enrollment> findByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") Enrollment.EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.schoolClass.id = :classId AND e.status = :status")
    List<Enrollment> findByClassIdAndStatus(
            @Param("classId") Long classId,
            @Param("status") Enrollment.EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.schoolClass.id = :classId AND e.status = 'ACTIVE'")
    Long countActiveEnrollmentsByClassId(@Param("classId") Long classId);

    boolean existsByStudentIdAndSchoolClassIdAndAcademicYear(Long studentId, Long classId, String academicYear);
}
