package com.eduflow.repository.academic;

import com.eduflow.entity.academic.TeacherClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherClassSubjectRepository extends JpaRepository<TeacherClassSubject, Long> {

    // Find all classes and subjects a teacher teaches
    List<TeacherClassSubject> findByTeacherIdAndActiveTrue(Long teacherId);

    // Find all classes a teacher teaches for a specific academic year
    List<TeacherClassSubject> findByTeacherIdAndAcademicYearAndActiveTrue(Long teacherId, String academicYear);

    // Find all teachers for a specific class
    List<TeacherClassSubject> findBySchoolClassIdAndActiveTrue(Long classId);

    // Find teacher assignment for specific class and subject
    Optional<TeacherClassSubject> findByTeacherIdAndSchoolClassIdAndSubjectIdAndAcademicYear(
            Long teacherId, Long classId, Long subjectId, String academicYear);

    // Check if a teacher teaches a specific subject in a class
    boolean existsByTeacherIdAndSchoolClassIdAndSubjectIdAndActiveTrue(
            Long teacherId, Long classId, Long subjectId);

    // Get all subjects a teacher teaches in a class
    @Query("SELECT tcs FROM TeacherClassSubject tcs WHERE tcs.teacher.id = :teacherId " +
            "AND tcs.schoolClass.id = :classId AND tcs.active = true")
    List<TeacherClassSubject> findByTeacherAndClass(@Param("teacherId") Long teacherId,
                                                     @Param("classId") Long classId);

    // Get distinct classes a teacher teaches
    @Query("SELECT DISTINCT tcs.schoolClass.id FROM TeacherClassSubject tcs " +
            "WHERE tcs.teacher.id = :teacherId AND tcs.active = true")
    List<Long> findDistinctClassIdsByTeacherId(@Param("teacherId") Long teacherId);
}