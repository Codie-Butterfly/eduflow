package com.eduflow.dto.response;

import com.eduflow.entity.academic.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponse {

    private Long id;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private String gradeLetter;
    private Grade.Term term;
    private String academicYear;
    private String teacherComment;

    private SubjectInfo subject;
    private StudentInfo student;
    private TeacherInfo gradedBy;

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
    public static class StudentInfo {
        private Long id;
        private String studentId;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long id;
        private String name;
    }
}
