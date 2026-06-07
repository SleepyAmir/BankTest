package com.springbank.user.dto;

import com.springbank.common.enums.KycLevel;

public record KycSubmitDto(
        Long userId,
        KycLevel requestedLevel,
        String nationalIdImagePath,
        String selfieImagePath,
        String addressProofPath
) {}
