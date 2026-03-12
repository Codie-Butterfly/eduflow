package com.eduflow.entity.communication;

import com.eduflow.entity.base.BaseEntity;
import com.eduflow.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    // For CLASS targeting - stores class IDs
    @ElementCollection
    @CollectionTable(name = "announcement_target_classes", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "class_id")
    @Builder.Default
    private List<Long> targetClassIds = new ArrayList<>();

    // For SPECIFIC_USERS targeting - stores user IDs
    @ElementCollection
    @CollectionTable(name = "announcement_target_users", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "user_id")
    @Builder.Default
    private List<Long> targetUserIds = new ArrayList<>();

    // For GRADE targeting - stores grade numbers (e.g., 1, 2, 3)
    @ElementCollection
    @CollectionTable(name = "announcement_target_grades", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "grade")
    @Builder.Default
    private List<Integer> targetGrades = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "announcement_attachments", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "attachment_url")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    public enum TargetType {
        ALL,
        CLASS,
        GRADE,
        PARENTS,
        TEACHERS,
        STUDENTS,
        SPECIFIC_USERS
    }

    public enum AnnouncementStatus {
        DRAFT,
        SCHEDULED,
        PUBLISHED,
        ARCHIVED
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public void publish() {
        this.status = AnnouncementStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }
}
