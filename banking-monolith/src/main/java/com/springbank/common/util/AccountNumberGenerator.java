package com.springbank.common.util;

import java.security.SecureRandom;

/**
 * تولید شماره حساب یکتا به فرمت IBAN-مانند ایران (IR + ۲۴ رقم).
 */
public final class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNumberGenerator() {
    }

    /** تولید شماره حساب ۲۶ کاراکتری: "IR" + ۲۴ رقم. */
    public static String generate() {
        StringBuilder sb = new StringBuilder("IR");
        for (int i = 0; i < 24; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
