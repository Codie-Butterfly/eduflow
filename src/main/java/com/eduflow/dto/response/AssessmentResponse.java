package com.eduflow.dto.response;

import com.eduflow.entity.academic.Assessment;
import com.eduflow.entity.academic.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponse {
    private Long id;
    private String title;
    private Assessment.AssessmentType type;
    private LocalDate date;
    private BigDecimal maxScore;
    private Grade.Term term;
    private String academicYear;
    private String description;

    private ClassInfo schoolClass;
    private SubjectInfo subject;
    private TeacherInfo teacher;

    private int totalStudents;
    private int scoredStudents;
    private BigDecimal averageScore;
    private BigDecimal averagePercentage;

    private List<ScoreInfo> scores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private Long id;
        private String name;
        private Integer grade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectInfo {
        private Long id;
        private String name;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreInfo {
        private Long id;
        private Long studentId;
        private String studentName;
        private String studentNumber;
        private BigDecimal score;
        private BigDecimal percentage;
        private String gradeLetter;
        private String remarks;
        private Boolean absent;
    }
}