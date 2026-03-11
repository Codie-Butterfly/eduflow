package com.eduflow.controller;

import com.eduflow.dto.response.MessageResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.entity.communication.Notification;
import com.eduflow.entity.user.User;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.communication.NotificationRepository;
import com.eduflow.repository.user.UserRepository;
import com.eduflow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all notifications", description = "Get paginated list of notifications for the current user")
    public ResponseEntity<PagedResponse<NotificationResponse>> getAllNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "sentAt") Pageable pageable) {
        User user = getUserFromDetails(userDetails);
        Page<Notification> page = notificationRepository.findByRecipientIdOrderBySentAtDesc(user.getId(), pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Get list of unread notifications")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        List<Notification> notifications = notificationService.getUnreadNotifications(user.getId());
        List<NotificationResponse> response = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        return ResponseEntity.ok(notificationRepository.countUnreadByRecipientId(user.getId()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a single notification as read")
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Verify the notification belongs to the user
        User user = getUserFromDetails(userDetails);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification", "id", id);
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok(MessageResponse.success("Notification marked as read"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<MessageResponse> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(MessageResponse.success("All notifications marked as read"));
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()));
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .channel(notification.getChannel() != null ? notification.getChannel().name() : null)
                .read(notification.isRead())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationResponse {
        private Long id;
        private String title;
        private String message;
        private String type;
        private String channel;
        private boolean read;
        private java.time.LocalDateTime sentAt;
        private java.time.LocalDateTime readAt;
        private String referenceType;
        private Long referenceId;
    }
}