package com.springbank.transaction.write.service;

import com.springbank.common.enums.TransactionStatus;
import com.springbank.common.enums.TransactionType;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.transaction.write.dto.TransactionResponseDto;
import com.springbank.transaction.write.dto.request.TransactionCreateDto;
import com.springbank.transaction.write.entity.Transaction;
import com.springbank.transaction.write.mapper.TransactionMapper;
import com.springbank.transaction.write.messaging.TransactionEventPublisher;
import com.springbank.transaction.write.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionWriteService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    @Value("${services.monolith.url:http://localhost:8081}")
    private String monolithBaseUrl;

    public TransactionResponseDto createTransaction(TransactionCreateDto dto) {
        // Balance check for transfer/withdrawal
        if (dto.fromAccountId() != null && (dto.type() == TransactionType.TRANSFER || dto.type() == TransactionType.WITHDRAWAL || dto.type() == TransactionType.CARD_PAYMENT)) {
            BigDecimal balance = checkBalance(dto.fromAccountId());
            if (balance.compareTo(dto.amount()) < 0) {
                throw new IllegalArgumentException("Insufficient balance");
            }
        }

        Transaction tx = transactionMapper.toEntity(dto);
        tx.setTrackingCode(generateTrackingCode());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCurrency(dto.currency() != null ? dto.currency() : "IRR");

        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction created: {}", saved.getId());

        // Publish event
        eventPublisher.publishTransactionCompleted(saved);

        return transactionMapper.toDto(saved);
    }

    public TransactionResponseDto completeTransaction(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction must be in PENDING status to complete. Current status: " + tx.getStatus());
        }

        // Update balances in monolith for transfer/withdrawal
        if (tx.getFromAccountId() != null && (tx.getType() == TransactionType.TRANSFER || tx.getType() == TransactionType.WITHDRAWAL || tx.getType() == TransactionType.CARD_PAYMENT)) {
            callWithdraw(tx.getFromAccountId(), tx.getAmount());
        }
        if (tx.getToAccountId() != null && tx.getType() == TransactionType.TRANSFER) {
            callDeposit(tx.getToAccountId(), tx.getAmount());
        }
        if (tx.getType() == TransactionType.DEPOSIT && tx.getToAccountId() != null) {
            callDeposit(tx.getToAccountId(), tx.getAmount());
        }

        tx.setStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(tx);
        eventPublisher.publishTransactionCompleted(saved);
        log.info("Transaction completed and balances updated: {}", id);
        return transactionMapper.toDto(saved);
    }

    public TransactionResponseDto failTransaction(Long id, String reason) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        tx.setStatus(TransactionStatus.FAILED);
        tx.setDescription(tx.getDescription() + " | Failed: " + reason);
        return transactionMapper.toDto(transactionRepository.save(tx));
    }

    public TransactionResponseDto reverseTransaction(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (tx.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Only completed transactions can be reversed. Current status: " + tx.getStatus());
        }

        // Reverse balances in monolith
        if (tx.getFromAccountId() != null && (tx.getType() == TransactionType.TRANSFER || tx.getType() == TransactionType.WITHDRAWAL || tx.getType() == TransactionType.CARD_PAYMENT)) {
            callDeposit(tx.getFromAccountId(), tx.getAmount());
        }
        if (tx.getToAccountId() != null && tx.getType() == TransactionType.TRANSFER) {
            callWithdraw(tx.getToAccountId(), tx.getAmount());
        }
        if (tx.getType() == TransactionType.DEPOSIT && tx.getToAccountId() != null) {
            callWithdraw(tx.getToAccountId(), tx.getAmount());
        }

        tx.setStatus(TransactionStatus.REVERSED);
        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction reversed and balances restored: {}", id);
        return transactionMapper.toDto(saved);
    }

    private void callDeposit(Long accountId, BigDecimal amount) {
        try {
            restTemplate.postForEntity(
                    monolithBaseUrl + "/internal/accounts/" + accountId + "/deposit?amount=" + amount,
                    null, String.class);
        } catch (Exception e) {
            log.error("Failed to deposit to account {}: {}", accountId, e.getMessage());
            throw new IllegalStateException("Failed to update account balance during deposit");
        }
    }

    private void callWithdraw(Long accountId, BigDecimal amount) {
        try {
            restTemplate.postForEntity(
                    monolithBaseUrl + "/internal/accounts/" + accountId + "/withdraw?amount=" + amount,
                    null, String.class);
        } catch (Exception e) {
            log.error("Failed to withdraw from account {}: {}", accountId, e.getMessage());
            throw new IllegalStateException("Failed to update account balance during withdraw");
        }
    }

    public TransactionResponseDto getById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private BigDecimal checkBalance(Long accountId) {
        try {
            var response = restTemplate.getForObject(
                    monolithBaseUrl + "/internal/accounts/" + accountId + "/balance",
                    java.util.Map.class);
            if (response != null && response.get("balance") != null) {
                return new BigDecimal(response.get("balance").toString());
            }
        } catch (Exception e) {
            log.error("Failed to check balance for account {}: {}", accountId, e.getMessage());
            throw new IllegalStateException("Cannot verify account balance");
        }
        return BigDecimal.ZERO;
    }

    private String generateTrackingCode() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
