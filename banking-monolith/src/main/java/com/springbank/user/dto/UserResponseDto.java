package com.springbank.user.dto;

public record UserResponseDto(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    String profilePictureUrl,
    boolean enabled,
    boolean emailVerified
) {}
