package com.springbank.transaction.read.repository;

import com.springbank.transaction.read.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTrackingCode(String trackingCode);
    List<Transaction> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    /** تراکنش‌های یک کاربر (که userId روی آن ثبت شده) — جدیدترین‌ها اول. */
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findByCardId(Long cardId);
}
