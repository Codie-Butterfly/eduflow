package com.eduflow.repository.finance;

import com.eduflow.entity.finance.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    List<Fee> findByAcademicYear(String academicYear);

    List<Fee> findByAcademicYearAndTerm(String academicYear, Fee.Term term);

    List<Fee> findByCategoryId(Long categoryId);

    List<Fee> findByActive(boolean active);

    @Query("SELECT f FROM Fee f JOIN f.applicableClasses c WHERE c.id = :classId AND f.academicYear = :academicYear")
    List<Fee> findByClassIdAndAcademicYear(@Param("classId") Long classId, @Param("academicYear") String academicYear);

    @Query("SELECT f FROM Fee f WHERE f.mandatory = true AND f.active = true AND f.academicYear = :academicYear")
    List<Fee> findMandatoryFees(@Param("academicYear") String academicYear);

    @Query("SELECT f FROM Fee f JOIN f.applicableClasses c WHERE c.grade = :grade AND f.academicYear = :academicYear")
    List<Fee> findByGradeAndAcademicYear(@Param("grade") Integer grade, @Param("academicYear") String academicYear);
}
