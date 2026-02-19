package com.eduflow.dto.request;

import com.eduflow.entity.academic.Grade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGradeRequest {

    @NotNull(message = "Enrollment ID is required")
    private Long enrollmentId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Score is required")
    @Positive(message = "Score must be positive")
    private BigDecimal score;

    @NotNull(message = "Max score is required")
    @Positive(message = "Max score must be positive")
    private BigDecimal maxScore;

    @NotNull(message = "Term is required")
    private Grade.Term term;

    @NotNull(message = "Academic year is required")
    private String academicYear;

    private String teacherComment;
}
