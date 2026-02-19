package com.eduflow.repository.communication;

import com.eduflow.entity.communication.Homework;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    List<Homework> findByTeacherId(Long teacherId);

    List<Homework> findBySchoolClassId(Long classId);

    List<Homework> findBySubjectId(Long subjectId);

    @Query("SELECT h FROM Homework h WHERE h.schoolClass.id = :classId AND h.status = 'ACTIVE' " +
            "ORDER BY h.dueDate ASC")
    Page<Homework> findActiveByClassId(@Param("classId") Long classId, Pageable pageable);

    @Query("SELECT h FROM Homework h WHERE h.schoolClass.id = :classId AND h.subject.id = :subjectId " +
            "AND h.academicYear = :academicYear")
    List<Homework> findByClassAndSubjectAndYear(
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId,
            @Param("academicYear") String academicYear);

    @Query("SELECT h FROM Homework h WHERE h.teacher.id = :teacherId AND h.status = 'ACTIVE'")
    List<Homework> findActiveByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT h FROM Homework h WHERE h.dueDate = :date AND h.status = 'ACTIVE'")
    List<Homework> findDueOn(@Param("date") LocalDate date);

    @Query("SELECT h FROM Homework h WHERE h.dueDate < :date AND h.status = 'ACTIVE'")
    List<Homework> findOverdue(@Param("date") LocalDate date);
}
