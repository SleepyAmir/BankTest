package com.springbank.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * درخواست ورود کاربر.
 */
public record LoginRequestDto(

        @NotBlank(message = "نام کاربری الزامی است")
        String username,

        @NotBlank(message = "رمز عبور الزامی است")
        String password
) {}
