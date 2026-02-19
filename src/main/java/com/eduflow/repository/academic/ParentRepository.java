package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByUserId(Long userId);

    @Query("SELECT p FROM Parent p WHERE LOWER(p.user.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(p.user.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Parent> searchByName(@Param("name") String name);

    @Query("SELECT p FROM Parent p JOIN p.children s WHERE s.currentClass.id = :classId")
    List<Parent> findByChildrenClassId(@Param("classId") Long classId);
}
