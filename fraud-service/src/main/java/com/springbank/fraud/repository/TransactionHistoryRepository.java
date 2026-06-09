package com.springbank.fraud.repository;

import com.springbank.fraud.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    /** تعداد تراکنش‌های کاربر از یک زمان مشخص به بعد (برای Velocity_Check). */
    long countByUserIdAndOccurredAtAfter(Long userId, LocalDateTime since);

    /** میانگین مبلغ تراکنش‌های کاربر در یک بازه (برای Amount-Anomaly). */
    @Query("SELECT AVG(h.amount) FROM TransactionHistory h " +
            "WHERE h.userId = :userId AND h.occurredAt >= :since")
    BigDecimal averageAmountSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** تراکنش‌های اخیر کاربر به‌ترتیب نزولی زمان (برای Structuring و Round-Amount متوالی). */
    List<TransactionHistory> findTop10ByUserIdOrderByOccurredAtDesc(Long userId);
}
