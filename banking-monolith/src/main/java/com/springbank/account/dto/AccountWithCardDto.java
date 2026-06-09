package com.springbank.account.dto;

import com.springbank.card.dto.IssuedCardDto;

/**
 * نتیجه‌ی افتتاح حساب: اطلاعات حساب به‌همراه کارت مجازی تازه صادرشده (فلوی ۳).
 */
public record AccountWithCardDto(
        AccountResponseDto account,
        IssuedCardDto card
) {}
