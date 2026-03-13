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
public class TeacherAssignmentResponse {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String academicYear;
    private ClassInfo schoolClass;
    private SubjectInfo subject;
    private Boolean active;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private Long id;
        private String name;
        private Integer grade;
        private String section;
        private int studentCount;
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

    // Summary view - classes with their subjects
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassWithSubjects {
        private Long classId;
        private String className;
        private Integer grade;
        private String section;
        private int studentCount;
        private List<SubjectInfo> subjects;
    }
}