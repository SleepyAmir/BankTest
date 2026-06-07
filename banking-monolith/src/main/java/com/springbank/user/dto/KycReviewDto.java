package com.springbank.user.dto;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;

public record KycReviewDto(
        KycStatus status,
        KycLevel approvedLevel,
        String rejectionReason
) {}
