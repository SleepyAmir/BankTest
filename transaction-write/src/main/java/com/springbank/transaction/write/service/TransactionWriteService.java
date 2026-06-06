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
    private final RestTemplate restTemplate = new RestTemplate();

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
        tx.setStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(tx);
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
        tx.setStatus(TransactionStatus.REVERSED);
        return transactionMapper.toDto(transactionRepository.save(tx));
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
