package com.eduflow.entity.academic;

import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "grades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "subject_id", "term"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "grade_letter")
    private String gradeLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private Teacher gradedBy;

    public enum Term {
        TERM_1, TERM_2, TERM_3, FINAL
    }

    public BigDecimal getPercentage() {
        if (score != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            return score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }
}
