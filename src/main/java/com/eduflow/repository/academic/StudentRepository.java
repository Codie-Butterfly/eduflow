package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByUserId(Long userId);

    boolean existsByStudentId(String studentId);

    @Query("SELECT s FROM Student s WHERE s.currentClass.id = :classId")
    List<Student> findByCurrentClassId(@Param("classId") Long classId);

    @Query("SELECT s FROM Student s WHERE s.parent.id = :parentId")
    List<Student> findByParentId(@Param("parentId") Long parentId);

    @Query("SELECT s FROM Student s WHERE s.status = :status")
    Page<Student> findByStatus(@Param("status") Student.StudentStatus status, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.currentClass.grade = :grade AND s.currentClass.academicYear = :academicYear")
    List<Student> findByGradeAndAcademicYear(@Param("grade") Integer grade, @Param("academicYear") String academicYear);

    @Query("SELECT s FROM Student s WHERE LOWER(s.user.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(s.user.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Student> searchByName(@Param("name") String name, Pageable pageable);
}
