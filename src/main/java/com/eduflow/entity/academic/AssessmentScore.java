package com.eduflow.entity.academic;

import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "assessment_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Builder.Default
    private Boolean absent = false;

    public BigDecimal getPercentage() {
        if (score != null && assessment != null && assessment.getMaxScore() != null
                && assessment.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            return score.multiply(BigDecimal.valueOf(100))
                    .divide(assessment.getMaxScore(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    public String getGradeLetter() {
        BigDecimal percentage = getPercentage();
        if (percentage == null) return "N/A";

        double pct = percentage.doubleValue();
        if (pct >= 90) return "A";
        if (pct >= 80) return "B";
        if (pct >= 70) return "C";
        if (pct >= 60) return "D";
        if (pct >= 50) return "E";
        return "F";
    }
}