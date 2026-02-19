package com.eduflow.repository.academic;

import com.eduflow.entity.academic.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findByAcademicYear(String academicYear);

    List<SchoolClass> findByGrade(Integer grade);

    List<SchoolClass> findByGradeAndAcademicYear(Integer grade, String academicYear);

    Page<SchoolClass> findByAcademicYear(String academicYear, Pageable pageable);

    Page<SchoolClass> findByGrade(Integer grade, Pageable pageable);

    Page<SchoolClass> findByGradeAndAcademicYear(Integer grade, String academicYear, Pageable pageable);

    @Query("SELECT c FROM SchoolClass c WHERE c.classTeacher.id = :teacherId")
    List<SchoolClass> findByClassTeacherId(@Param("teacherId") Long teacherId);

    Optional<SchoolClass> findByNameAndGradeAndAcademicYear(String name, Integer grade, String academicYear);

    boolean existsByNameAndGradeAndAcademicYear(String name, Integer grade, String academicYear);

    @Query("SELECT c FROM SchoolClass c JOIN c.subjects s WHERE s.id = :subjectId")
    List<SchoolClass> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT COUNT(s) FROM SchoolClass c JOIN c.students s WHERE c.id = :classId")
    Long countStudentsByClassId(@Param("classId") Long classId);
}
