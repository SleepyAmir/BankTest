package com.springbank.notification.dto;

import java.time.LocalDateTime;

public record NotificationDto(
    Long id,
    String type,
    String title,
    String message,
    boolean isRead,
    String channel,
    Long userId,
    LocalDateTime createdAt
) {}
