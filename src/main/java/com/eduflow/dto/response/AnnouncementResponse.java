package com.eduflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private String priority;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private List<String> attachments;
    private boolean read;
    private String senderName;
    // Admin fields
    private String status;
    private String targetType;
    private List<Long> targetClassIds;
    private List<Long> targetUserIds;
    private List<Integer> targetGrades;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}