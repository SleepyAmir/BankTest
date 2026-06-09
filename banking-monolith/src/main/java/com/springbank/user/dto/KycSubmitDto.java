package com.springbank.user.dto;

import com.springbank.common.enums.KycLevel;
import jakarta.validation.constraints.NotNull;

public record KycSubmitDto(

        @NotNull(message = "شناسه‌ی کاربر الزامی است")
        Long userId,

        KycLevel requestedLevel,

        String nationalIdImagePath,
        String selfieImagePath,
        String addressProofPath
) {}
