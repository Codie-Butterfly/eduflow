package com.eduflow.controller.admin;

import com.eduflow.dto.response.AnnouncementResponse;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.entity.communication.Announcement;
import com.eduflow.entity.user.User;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.entity.communication.AnnouncementRead;
import com.eduflow.repository.communication.AnnouncementReadRepository;
import com.eduflow.repository.communication.AnnouncementRepository;
import com.eduflow.repository.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Announcements", description = "Announcement management endpoints")
public class AdminAnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all announcements", description = "Get paginated list of all announcements")
    public ResponseEntity<PagedResponse<AnnouncementResponse>> getAllAnnouncements(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<Announcement> page = announcementRepository.findAll(pageable);
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get announcements by status", description = "Get announcements filtered by status")
    public ResponseEntity<PagedResponse<AnnouncementResponse>> getAnnouncementsByStatus(
            @PathVariable Announcement.AnnouncementStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Announcement> page = announcementRepository.findByStatusOrderByPublishedAtDesc(status, pageable);
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get announcement by ID", description = "Get announcement details")
    public ResponseEntity<AnnouncementResponse> getAnnouncementById(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        return ResponseEntity.ok(mapToResponse(announcement));
    }

    @PostMapping
    @Operation(summary = "Create announcement", description = "Create a new announcement")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User sender = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()));

        Announcement announcement = Announcement.builder()
                .sender(sender)
                .title(request.getTitle())
                .content(request.getContent())
                .targetType(request.getTargetType())
                .targetClassIds(request.getTargetClassIds() != null ? request.getTargetClassIds() : List.of())
                .targetUserIds(request.getTargetUserIds() != null ? request.getTargetUserIds() : List.of())
                .targetGrades(request.getTargetGrades() != null ? request.getTargetGrades() : List.of())
                .attachments(request.getAttachments() != null ? request.getAttachments() : List.of())
                .priority(request.getPriority() != null ? request.getPriority() : Announcement.Priority.NORMAL)
                .scheduledAt(request.getScheduledAt())
                .expiresAt(request.getExpiresAt())
                .status(Announcement.AnnouncementStatus.DRAFT)
                .build();

        announcement = announcementRepository.save(announcement);
        return ResponseEntity.ok(mapToResponse(announcement));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update announcement", description = "Update an existing announcement")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setTargetType(request.getTargetType());
        if (request.getTargetClassIds() != null) {
            announcement.setTargetClassIds(request.getTargetClassIds());
        }
        if (request.getTargetUserIds() != null) {
            announcement.setTargetUserIds(request.getTargetUserIds());
        }
        if (request.getTargetGrades() != null) {
            announcement.setTargetGrades(request.getTargetGrades());
        }
        if (request.getAttachments() != null) {
            announcement.setAttachments(request.getAttachments());
        }
        if (request.getPriority() != null) {
            announcement.setPriority(request.getPriority());
        }
        announcement.setScheduledAt(request.getScheduledAt());
        announcement.setExpiresAt(request.getExpiresAt());

        announcement = announcementRepository.save(announcement);
        return ResponseEntity.ok(mapToResponse(announcement));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete announcement", description = "Delete an announcement")
    public ResponseEntity<MessageResponse> deleteAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        announcementRepository.delete(announcement);
        return ResponseEntity.ok(MessageResponse.success("Announcement deleted successfully"));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish announcement", description = "Publish a draft announcement")
    public ResponseEntity<AnnouncementResponse> publishAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcement.publish();
        announcement = announcementRepository.save(announcement);
        return ResponseEntity.ok(mapToResponse(announcement));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive announcement", description = "Archive a published announcement")
    public ResponseEntity<AnnouncementResponse> archiveAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcement.setStatus(Announcement.AnnouncementStatus.ARCHIVED);
        announcement = announcementRepository.save(announcement);
        return ResponseEntity.ok(mapToResponse(announcement));
    }

    @GetMapping("/{id}/reads")
    @Operation(summary = "Get announcement reads", description = "Get list of users who have read the announcement")
    public ResponseEntity<AnnouncementReadStatsResponse> getAnnouncementReads(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        List<AnnouncementRead> reads = announcementReadRepository.findByAnnouncementIdOrderByReadAtDesc(id);
        long totalReads = reads.size();

        List<AnnouncementReadStatsResponse.ReadInfo> readDetails = reads.stream()
                .map(r -> AnnouncementReadStatsResponse.ReadInfo.builder()
                        .userId(r.getUser().getId())
                        .userName(r.getUser().getFullName())
                        .email(r.getUser().getEmail())
                        .readAt(r.getReadAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(AnnouncementReadStatsResponse.builder()
                .announcementId(id)
                .announcementTitle(announcement.getTitle())
                .totalReads(totalReads)
                .reads(readDetails)
                .build());
    }

    private AnnouncementResponse mapToResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .priority(announcement.getPriority() != null ? announcement.getPriority().name() : null)
                .publishedAt(announcement.getPublishedAt())
                .expiresAt(announcement.getExpiresAt())
                .attachments(announcement.getAttachments())
                .read(false)
                .senderName(announcement.getSender() != null ? announcement.getSender().getFullName() : null)
                .status(announcement.getStatus() != null ? announcement.getStatus().name() : null)
                .targetType(announcement.getTargetType() != null ? announcement.getTargetType().name() : null)
                .targetClassIds(announcement.getTargetClassIds())
                .targetUserIds(announcement.getTargetUserIds())
                .targetGrades(announcement.getTargetGrades())
                .scheduledAt(announcement.getScheduledAt())
                .createdAt(announcement.getCreatedAt())
                .build();
    }

    @Data
    public static class CreateAnnouncementRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Content is required")
        private String content;

        @NotNull(message = "Target type is required")
        private Announcement.TargetType targetType;

        // For CLASS targeting - list of class IDs
        private List<Long> targetClassIds;

        // For SPECIFIC_USERS targeting - list of user IDs
        private List<Long> targetUserIds;

        // For GRADE targeting - list of grade numbers (e.g., 1, 2, 3)
        private List<Integer> targetGrades;

        private List<String> attachments;
        private Announcement.Priority priority;
        private LocalDateTime scheduledAt;
        private LocalDateTime expiresAt;
    }

    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AnnouncementReadStatsResponse {
        private Long announcementId;
        private String announcementTitle;
        private long totalReads;
        private List<ReadInfo> reads;

        @Data
        @lombok.Builder
        @lombok.AllArgsConstructor
        @lombok.NoArgsConstructor
        public static class ReadInfo {
            private Long userId;
            private String userName;
            private String email;
            private LocalDateTime readAt;
        }
    }
}
