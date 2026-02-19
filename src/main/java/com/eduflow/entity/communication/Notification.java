package com.eduflow.entity.communication;

import com.eduflow.entity.base.BaseEntity;
import com.eduflow.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Builder.Default
    @Column(name = "is_read")
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "delivery_error")
    private String deliveryError;

    public enum NotificationType {
        PAYMENT_REMINDER,
        PAYMENT_RECEIVED,
        FEE_ASSIGNED,
        GRADE_PUBLISHED,
        HOMEWORK_ASSIGNED,
        ANNOUNCEMENT,
        REPORT_PUBLISHED,
        ATTENDANCE,
        GENERAL
    }

    public enum NotificationChannel {
        IN_APP,
        EMAIL,
        SMS,
        PUSH
    }

    public enum DeliveryStatus {
        PENDING,
        SENT,
        DELIVERED,
        FAILED
    }

    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
