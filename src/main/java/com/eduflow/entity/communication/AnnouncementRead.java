package com.eduflow.entity.communication;

import com.eduflow.entity.base.BaseEntity;
import com.eduflow.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_reads", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"announcement_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}