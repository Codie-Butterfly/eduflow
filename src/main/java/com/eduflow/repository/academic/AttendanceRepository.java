package com.eduflow.repository.academic;

import com.eduflow.entity.academic.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySchoolClassIdAndDate(Long classId, LocalDate date);

    List<Attendance> findByStudentIdAndDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findBySchoolClassIdAndDateBetween(Long classId, LocalDate startDate, LocalDate endDate);

    Optional<Attendance> findByStudentIdAndSchoolClassIdAndDate(Long studentId, Long classId, LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'PRESENT' " +
            "AND a.date BETWEEN :startDate AND :endDate")
    long countPresentDays(@Param("studentId") Long studentId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'ABSENT' " +
            "AND a.date BETWEEN :startDate AND :endDate")
    long countAbsentDays(@Param("studentId") Long studentId,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId " +
            "AND a.date BETWEEN :startDate AND :endDate")
    long countTotalDays(@Param("studentId") Long studentId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
