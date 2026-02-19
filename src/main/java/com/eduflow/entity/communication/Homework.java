package com.eduflow.entity.communication;

import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Subject;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "homework")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Homework extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @ElementCollection
    @CollectionTable(name = "homework_attachments", joinColumns = @JoinColumn(name = "homework_id"))
    @Column(name = "attachment_url")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @Column(name = "max_score")
    private Integer maxScore;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.ACTIVE;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    private Term term;

    public enum HomeworkStatus {
        DRAFT,
        ACTIVE,
        CLOSED,
        GRADED
    }

    public enum Term {
        TERM_1, TERM_2, TERM_3
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate);
    }
}
