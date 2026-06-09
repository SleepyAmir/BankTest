package com.springbank.user.dto;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KycReviewDto(

        @NotNull(message = "نتیجه‌ی بررسی (APPROVED/REJECTED) الزامی است")
        KycStatus status,

        KycLevel approvedLevel,

        @Size(max = 500, message = "دلیل رد نمی‌تواند بیش از ۵۰۰ کاراکتر باشد")
        String rejectionReason
) {}
