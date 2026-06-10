package com.springbank.transaction.read.service;

import com.springbank.transaction.read.dto.response.TransactionResponseDto;
import com.springbank.transaction.read.entity.Transaction;
import com.springbank.transaction.read.mapper.TransactionMapper;
import com.springbank.transaction.read.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionReadService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Cacheable(value = "transactions", key = "#id")
    public TransactionResponseDto getById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    public List<TransactionResponseDto> getByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TransactionResponseDto> getByCardId(Long cardId) {
        return transactionRepository.findByCardId(cardId).stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TransactionResponseDto> getRecentTransactions() {
        return transactionRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    /** تراکنش‌های یک کاربر مشخص (برای پنل مشتری — فقط تراکنش‌های خودش). */
    public List<TransactionResponseDto> getByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "transactions", key = "#trackingCode")
    public TransactionResponseDto getByTrackingCode(String trackingCode) {
        return transactionRepository.findByTrackingCode(trackingCode)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + trackingCode));
    }
}
