package com.springbank.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(

        @NotBlank(message = "refresh token الزامی است")
        String refreshToken
) {}
