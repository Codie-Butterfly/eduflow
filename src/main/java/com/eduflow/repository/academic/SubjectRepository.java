package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    List<Subject> findByMandatory(boolean mandatory);

    @Query("SELECT s FROM Subject s JOIN s.classes c WHERE c.id = :classId")
    List<Subject> findByClassId(@Param("classId") Long classId);

    @Query("SELECT s FROM Subject s JOIN s.teachers t WHERE t.id = :teacherId")
    List<Subject> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT s FROM Subject s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Subject> searchByName(@Param("name") String name);
}
