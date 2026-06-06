package com.springbank.notification.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.notification.dto.NotificationDto;
import com.springbank.notification.entity.Notification;
import com.springbank.notification.service.NotificationService;
import com.springbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType() != null ? n.getType().name() : null,
                n.getTitle(),
                n.getMessage(),
                n.getIsRead(),
                n.getChannel() != null ? n.getChannel().name() : null,
                n.getUser() != null ? n.getUser().getId() : null,
                n.getCreatedAt()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getAll() {
        List<NotificationDto> list = notificationService.getAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All notifications", list, "/api/notifications"));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getByUser(@PathVariable Long userId) {
        List<NotificationDto> list = notificationService.getByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("User notifications", list, "/api/notifications/user/" + userId));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(@AuthenticationPrincipal UserDetails principal) {
        var user = userService.getUserByUsername(principal.getUsername());
        List<NotificationDto> list = notificationService.getByUserId(user.id()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("My notifications", list, "/api/notifications/me"));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", toDto(notificationService.markAsRead(id)), "/api/notifications/" + id + "/read"));
    }
}
