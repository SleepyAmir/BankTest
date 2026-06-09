package com.springbank.common.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * TRANSACTION BLOCKED EVENT
 * ============================================================================
 * توسط fraud-service منتشر می‌شود زمانی که قوانین تشخیص تقلب، سطح ریسک BLOCK را
 * تعیین می‌کنند. مصرف‌کننده‌ها (transaction-read / monolith / notification) می‌توانند
 * وضعیت تراکنش را به BLOCKED تغییر دهند و به کاربر اطلاع دهند.
 *
 * routing key: transaction.blocked
 * ============================================================================
 */
@Data
@Builder
public class TransactionBlockedEvent {
    private Long transactionId;
    private String trackingCode;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal riskScore;
    private String riskLevel;
    private List<String> triggeredRules;
    private String reason;
    private LocalDateTime blockedAt;
}
