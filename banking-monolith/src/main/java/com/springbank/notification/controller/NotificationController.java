package com.springbank.notification.controller;

import com.springbank.common.dto.ApiResponse;
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

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All notifications", notificationService.getAll(), "/api/notifications"));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<List<Notification>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User notifications", notificationService.getByUserId(userId), "/api/notifications/user/" + userId));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@AuthenticationPrincipal UserDetails principal) {
        var user = userService.getUserByUsername(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("My notifications", notificationService.getByUserId(user.id()), "/api/notifications/me"));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markAsRead(id), "/api/notifications/" + id + "/read"));
    }
}
