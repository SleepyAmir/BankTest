package com.springbank.user.dto;

import com.springbank.common.enums.KycLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record KycSubmitDto(

        @NotNull(message = "شناسه‌ی کاربر الزامی است")
        Long userId,

        KycLevel requestedLevel,

        @Pattern(regexp = "^\\d{10}$", message = "کد ملی باید ۱۰ رقم باشد")
        String nationalCode,

        String birthDate,
        String address,
        String postalCode,

        String nationalIdImagePath,
        String selfieImagePath,
        String addressProofPath
) {}
