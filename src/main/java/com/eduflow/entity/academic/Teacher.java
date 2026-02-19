package com.eduflow.entity.academic;

import com.eduflow.entity.base.BaseEntity;
import com.eduflow.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher extends BaseEntity {

    @Column(name = "employee_id", unique = true, nullable = false)
    private String employeeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String qualification;

    private String specialization;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(columnDefinition = "TEXT")
    private String address;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();

    @OneToMany(mappedBy = "classTeacher", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SchoolClass> assignedClasses = new HashSet<>();

    public void addSubject(Subject subject) {
        subjects.add(subject);
        subject.getTeachers().add(this);
    }

    public void removeSubject(Subject subject) {
        subjects.remove(subject);
        subject.getTeachers().remove(this);
    }
}
