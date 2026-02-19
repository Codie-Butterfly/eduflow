package com.eduflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponse {

    private Long id;
    private String name;
    private Integer grade;
    private String academicYear;
    private String section;
    private Integer maxCapacity;
    private Integer currentEnrollment;

    private TeacherSummary classTeacher;
    private List<SubjectSummary> subjects;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private Long id;
        private String employeeId;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectSummary {
        private Long id;
        private String name;
        private String code;
    }
}
