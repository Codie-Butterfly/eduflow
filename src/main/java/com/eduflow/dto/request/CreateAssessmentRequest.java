package com.eduflow.dto.request;

import com.eduflow.entity.academic.Assessment;
import com.eduflow.entity.academic.Grade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateAssessmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Assessment type is required")
    private Assessment.AssessmentType type;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Maximum score is required")
    @Positive(message = "Maximum score must be positive")
    private BigDecimal maxScore;

    @NotNull(message = "Term is required")
    private Grade.Term term;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private String description;

    // Optional: include scores when creating assessment
    private List<StudentScore> scores;

    @Data
    public static class StudentScore {
        @NotNull(message = "Student ID is required")
        private Long studentId;
        private BigDecimal score;
        private String remarks;
        private Boolean absent;
    }
}