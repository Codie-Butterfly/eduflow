package com.eduflow.dto.response;

import com.eduflow.entity.communication.Homework;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private List<String> attachments;
    private Integer maxScore;
    private Homework.HomeworkStatus status;
    private String academicYear;
    private Homework.Term term;
    private boolean overdue;

    private SubjectInfo subject;
    private ClassInfo schoolClass;
    private TeacherInfo teacher;

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
    public static class ClassInfo {
        private Long id;
        private String name;
        private Integer grade;
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
