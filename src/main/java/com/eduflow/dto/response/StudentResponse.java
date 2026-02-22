package com.eduflow.dto.response;

import com.eduflow.entity.academic.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String studentId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private Student.Gender gender;
    private LocalDate enrollmentDate;
    private String address;
    private String bloodGroup;
    private String medicalConditions;
    private Student.StudentStatus status;

    private ClassSummary currentClass;
    private ParentSummary parent;

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
    public static class ParentSummary {
        private Long id;
        private String name;
        private String phone;
        private String email;
    }
}
