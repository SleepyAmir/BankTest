package com.springbank.common.util;

import java.security.SecureRandom;

/**
 * ============================================================================
 * CARD NUMBER GENERATOR — تولید شماره کارت ۱۶ رقمی معتبر (Luhn)، CVV2 و PIN
 * ============================================================================
 * شماره کارت با پیشوند BIN بانک ساخته می‌شود و رقم کنترلی آن طبق الگوریتم Luhn
 * محاسبه می‌شود تا یک شماره کارت ساختاراً معتبر باشد.
 * ============================================================================
 */
public final class CardNumberGenerator {

    /** BIN نمونه‌ی بانک (۶ رقم). */
    private static final String BANK_BIN = "627412";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CardNumberGenerator() {
    }

    /** تولید شماره کارت ۱۶ رقمی با رقم کنترلی معتبر Luhn. */
    public static String generateCardNumber() {
        StringBuilder sb = new StringBuilder(BANK_BIN);
        // ۹ رقم تصادفی (۶ + ۹ + ۱ رقم کنترلی = ۱۶)
        for (int i = 0; i < 9; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        int checkDigit = luhnCheckDigit(sb.toString());
        sb.append(checkDigit);
        return sb.toString();
    }

    /** تولید CVV2 سه رقمی. */
    public static String generateCvv2() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    /** تولید PIN چهار رقمی. */
    public static String generatePin() {
        return String.format("%04d", RANDOM.nextInt(10000));
    }

    /** اعتبارسنجی Luhn یک شماره‌ی کامل. */
    public static boolean isValidLuhn(String number) {
        if (number == null || !number.matches("\\d{13,19}")) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /** محاسبه‌ی رقم کنترلی Luhn برای یک پیشوند (بدون رقم کنترلی). */
    private static int luhnCheckDigit(String partialNumber) {
        int sum = 0;
        boolean alternate = true; // چون رقم کنترلی در انتها اضافه می‌شود، از راست شروع و alternate=true
        for (int i = partialNumber.length() - 1; i >= 0; i--) {
            int digit = partialNumber.charAt(i) - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }
}
