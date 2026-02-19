package com.eduflow.entity.report;

import com.eduflow.entity.academic.Enrollment;
import com.eduflow.entity.academic.Grade;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "term", "academic_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "report_grades",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "grade_id")
    )
    @Builder.Default
    private Set<Grade> grades = new HashSet<>();

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "total_students")
    private Integer totalStudents;

    @Column(name = "attendance_percentage", precision = 5, scale = 2)
    private BigDecimal attendancePercentage;

    @Column(name = "days_present")
    private Integer daysPresent;

    @Column(name = "days_absent")
    private Integer daysAbsent;

    @Column(name = "class_teacher_comment", columnDefinition = "TEXT")
    private String classTeacherComment;

    @Column(name = "principal_comment", columnDefinition = "TEXT")
    private String principalComment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public enum Term {
        TERM_1, TERM_2, TERM_3, FINAL
    }

    public enum ReportStatus {
        DRAFT,
        UNDER_REVIEW,
        APPROVED,
        PUBLISHED
    }

    public void publish() {
        this.status = ReportStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void calculateStatistics() {
        if (grades == null || grades.isEmpty()) {
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (Grade grade : grades) {
            if (grade.getScore() != null) {
                total = total.add(grade.getScore());
                count++;
            }
        }

        this.totalScore = total;
        if (count > 0) {
            this.averageScore = total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
        }
    }
}
