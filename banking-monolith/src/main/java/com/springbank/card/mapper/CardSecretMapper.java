package com.springbank.card.mapper;

import com.springbank.common.security.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * کمک‌کننده برای رمزگشایی CVV2 هنگام نگاشت Card → CardResponseDto.
 * توسط MapStruct (از طریق uses) استفاده می‌شود.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardSecretMapper {

    private final EncryptionUtils encryptionUtils;

    /** رمزگشایی CVV2 ذخیره‌شده (در صورت خطا، مقدار ماسک‌شده برمی‌گرداند). */
    @Named("decryptCvv")
    public String decryptCvv(String encryptedCvv) {
        if (encryptedCvv == null || encryptedCvv.isBlank()) {
            return null;
        }
        try {
            return encryptionUtils.decrypt(encryptedCvv);
        } catch (Exception e) {
            // اگر مقدار قدیمی (هش‌شده) یا با کلید دیگری رمزنگاری شده باشد، رمزگشایی ممکن نیست
            log.debug("[CARD] رمزگشایی CVV ممکن نشد (احتمالاً داده‌ی قدیمی): {}", e.getMessage());
            return "•••";
        }
    }
}
