package com.springbank.user.dto;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KycVerificationDto(
        Long id,
        Long userId,
        String username,
        KycStatus status,
        KycLevel level,
        String nationalCode,
        String birthDate,
        String address,
        String postalCode,
        String nationalIdImagePath,
        String selfieImagePath,
        String addressProofPath,
        String rejectionReason,
        String verifiedBy,
        LocalDateTime verifiedAt,
        BigDecimal dailyTransferLimit,
        BigDecimal monthlyTransferLimit
) {}
