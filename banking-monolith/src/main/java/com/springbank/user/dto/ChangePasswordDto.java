package com.springbank.user.dto;

public record ChangePasswordDto(
    String currentPassword,
    String newPassword
) {}
