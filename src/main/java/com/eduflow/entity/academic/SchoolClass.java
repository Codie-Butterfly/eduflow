package com.eduflow.entity.academic;

import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "school_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClass extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer grade;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    private String section;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "class_subjects",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();

    @OneToMany(mappedBy = "currentClass", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Student> students = new HashSet<>();

    @OneToMany(mappedBy = "schoolClass", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    public void addSubject(Subject subject) {
        subjects.add(subject);
        subject.getClasses().add(this);
    }

    public void removeSubject(Subject subject) {
        subjects.remove(subject);
        subject.getClasses().remove(this);
    }

    public String getFullName() {
        return "Grade " + grade + " - " + name + (section != null ? " (" + section + ")" : "");
    }
}
