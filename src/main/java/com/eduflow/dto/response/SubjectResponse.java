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
public class SubjectResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer creditHours;
    private boolean mandatory;
    private List<ClassSummary> classes;
    private List<TeacherSummary> teachers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSummary {
        private Long id;
        private String name;
        private Integer grade;
        private String academicYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private Long id;
        private String name;
        private String employeeId;
    }
}
