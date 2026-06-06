package com.springbank.transaction.write.repository;

import com.springbank.transaction.write.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTrackingCode(String trackingCode);
    List<Transaction> findByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);
    List<Transaction> findByCardId(Long cardId);
    List<Transaction> findTop100ByOrderByCreatedAtDesc();
}
