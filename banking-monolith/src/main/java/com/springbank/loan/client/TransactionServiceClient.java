package com.springbank.loan.client;

import com.springbank.common.enums.TransactionType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * TRANSACTION SERVICE CLIENT — فراخوانی transaction-write از monolith
 * ============================================================================
 * برای ثبت تراکنش‌های مرتبط با وام (واریز وام و پرداخت قسط) از طریق «تنها مرجع
 * تراکنش» یعنی transaction-write استفاده می‌شود تا جابجایی پول اتمیک باشد و
 * تراکنش در read model/fraud/analytics ثبت شود.
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.transaction-write.url:http://localhost:8088}")
    private String transactionWriteUrl;

    /**
     * ثبت یک تراکنش از طریق transaction-write (که خودش جابجایی پول را اتمیک انجام می‌دهد).
     *
     * @return true اگر موفق بود.
     */
    @CircuitBreaker(name = "transactionWriteService", fallbackMethod = "createTransactionFallback")
    public boolean createTransaction(TransactionType type, BigDecimal amount,
                                     Long fromAccountId, Long toAccountId,
                                     Long userId, String category, String description) {
        String url = transactionWriteUrl + "/api/transactions";
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("currency", "IRR");
        body.put("type", type.name());
        body.put("description", description);
        body.put("fromAccountId", fromAccountId);
        body.put("toAccountId", toAccountId);
        body.put("spendingCategory", category);
        body.put("userId", userId);
        
        restTemplate.postForEntity(url, body, String.class);
        log.info("[LOAN-TX] ✅ تراکنش {} از طریق transaction-write ثبت شد (amount={})", type, amount);
        return true;
    }

    public boolean createTransactionFallback(TransactionType type, BigDecimal amount,
                                             Long fromAccountId, Long toAccountId,
                                             Long userId, String category, String description, Exception e) {
        log.error("[LOAN-TX] ⚠️ Circuit Breaker FALLBACK: ثبت تراکنش {} ناموفق بود. دلیل: {}", type, e.getMessage());
        return false;
    }
}
