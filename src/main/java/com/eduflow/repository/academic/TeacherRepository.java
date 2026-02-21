package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeId(String employeeId);

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByEmployeeId(String employeeId);

    @Query("SELECT t FROM Teacher t JOIN t.subjects s WHERE s.id = :subjectId")
    List<Teacher> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT t FROM Teacher t JOIN t.assignedClasses c WHERE c.id = :classId")
    Optional<Teacher> findClassTeacher(@Param("classId") Long classId);

    @Query("SELECT t FROM Teacher t WHERE LOWER(t.user.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(t.user.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Teacher> searchByName(@Param("name") String name);

    @Query("SELECT MAX(CAST(SUBSTRING(t.employeeId, 8) AS int)) FROM Teacher t WHERE t.employeeId LIKE CONCAT(:prefix, '%')")
    Integer findMaxEmployeeIdNumber(@Param("prefix") String prefix);
}
