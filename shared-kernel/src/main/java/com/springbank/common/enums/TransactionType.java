package com.springbank.common.enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    CARD_PAYMENT,
    REFUND,
    LOAN_DISBURSEMENT, // واریز مبلغ وام به حساب کاربر (فلوی ۹)
    LOAN_PAYMENT       // پرداخت قسط وام (فلوی ۱۰)
}
