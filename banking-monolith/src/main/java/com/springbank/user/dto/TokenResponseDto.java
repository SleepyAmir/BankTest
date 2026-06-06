package com.springbank.user.dto;

public record TokenResponseDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn
) {}
