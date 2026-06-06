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

/**
 * ============================================================================
 * TRANSACTION WRITE SERVICE — CQRS Write Model
 * ============================================================================
 * Flow: Create TX → Check Balance (calls Monolith 8081) → Save PENDING
 *       → Publish Event to RabbitMQ → Complete TX → Update Balances → Publish Event
 *
 * ERROR TRACING: If something fails, check logs for these markers:
 *   [TX-CREATE] [TX-BALANCE] [TX-SAVE] [TX-PUBLISH] [TX-COMPLETE] [TX-REVERSE]
 * ============================================================================
 */
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

    /**
     * Step 1: Create a new transaction in PENDING status
     * - Checks balance for TRANSFER/WITHDRAWAL/CARD_PAYMENT (calls Monolith)
     * - Generates tracking code
     * - Publishes TransactionCompletedEvent to RabbitMQ
     *
     * ERRORS:
     *   "Insufficient balance" → Account balance < amount
     *   "Cannot verify account balance" → Monolith (8081) is down or account not found
     *   "Failed to publish event" → RabbitMQ is down
     */
    public TransactionResponseDto createTransaction(TransactionCreateDto dto) {
        log.info("[TX-CREATE] ==================== NEW TRANSACTION REQUEST ====================");
        log.info("[TX-CREATE] Type={}, Amount={}, FromAccount={}, ToAccount={}, CardId={}",
                dto.type(), dto.amount(), dto.fromAccountId(), dto.toAccountId(), dto.cardId());

        // Step 1a: Balance validation for outgoing transactions
        if (dto.fromAccountId() != null &&
            (dto.type() == TransactionType.TRANSFER || dto.type() == TransactionType.WITHDRAWAL || dto.type() == TransactionType.CARD_PAYMENT)) {
            log.info("[TX-BALANCE] Checking balance for accountId={}", dto.fromAccountId());
            log.info("[TX-BALANCE] Calling Monolith internal API: GET {}/internal/accounts/{}/balance", monolithBaseUrl, dto.fromAccountId());

            BigDecimal balance = checkBalance(dto.fromAccountId());
            log.info("[TX-BALANCE] Account balance={}, Requested amount={}", balance, dto.amount());

            if (balance.compareTo(dto.amount()) < 0) {
                log.error("[TX-BALANCE] ❌ REJECTED: Insufficient balance ({} < {})", balance, dto.amount());
                throw new IllegalArgumentException("Insufficient balance. Available: " + balance + ", Requested: " + dto.amount());
            }
            log.info("[TX-BALANCE] ✅ Balance sufficient");
        } else {
            log.info("[TX-BALANCE] Skipping balance check (type={} does not require it)", dto.type());
        }

        // Step 1b: Build and save transaction
        Transaction tx = transactionMapper.toEntity(dto);
        tx.setTrackingCode(generateTrackingCode());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCurrency(dto.currency() != null ? dto.currency() : "IRR");

        log.info("[TX-SAVE] Saving transaction with trackingCode={}", tx.getTrackingCode());
        Transaction saved = transactionRepository.save(tx);
        log.info("[TX-SAVE] ✅ Transaction saved: id={}, trackingCode={}, status={}", saved.getId(), saved.getTrackingCode(), saved.getStatus());

        // Step 1c: Publish event to RabbitMQ for other services
        log.info("[TX-PUBLISH] Publishing TransactionCompletedEvent to RabbitMQ...");
        try {
            eventPublisher.publishTransactionCompleted(saved);
            log.info("[TX-PUBLISH] ✅ Event published successfully. Other services (read, fraud, analytics, audit, notification) will process it.");
        } catch (Exception e) {
            log.error("[TX-PUBLISH] ❌ FAILED to publish event! RabbitMQ may be down. Error: {}", e.getMessage());
            log.error("[TX-PUBLISH] ⚠️ Transaction is saved but other services (fraud, analytics, audit) won't see it!");
            // Don't throw here — we still return the created transaction
        }

        log.info("[TX-CREATE] ==================== TRANSACTION CREATED SUCCESSFULLY ====================\n");
        return transactionMapper.toDto(saved);
    }

    /**
     * Step 2: Complete a PENDING transaction
     * - Calls Monolith to actually move money (withdraw from source, deposit to destination)
     * - Updates status to COMPLETED
     * - Publishes another event
     *
     * ERRORS:
     *   "Transaction must be in PENDING status" → Already completed/failed/reversed
     *   "Failed to update account balance" → Monolith is down during balance update
     */
    public TransactionResponseDto completeTransaction(Long id) {
        log.info("[TX-COMPLETE] ==================== COMPLETING TRANSACTION id={} ====================", id);

        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("[TX-COMPLETE] ❌ Transaction not found with id={}", id);
                    return new ResourceNotFoundException("Transaction", id);
                });
        log.info("[TX-COMPLETE] Found transaction: trackingCode={}, type={}, status={}, amount={}",
                tx.getTrackingCode(), tx.getType(), tx.getStatus(), tx.getAmount());

        if (tx.getStatus() != TransactionStatus.PENDING) {
            log.error("[TX-COMPLETE] ❌ Cannot complete transaction. Current status={} (expected PENDING)", tx.getStatus());
            throw new IllegalStateException("Transaction must be in PENDING status to complete. Current status: " + tx.getStatus());
        }

        // Step 2a: Withdraw from source account
        if (tx.getFromAccountId() != null &&
            (tx.getType() == TransactionType.TRANSFER || tx.getType() == TransactionType.WITHDRAWAL || tx.getType() == TransactionType.CARD_PAYMENT)) {
            log.info("[TX-COMPLETE] Withdrawing {} from accountId={}", tx.getAmount(), tx.getFromAccountId());
            callWithdraw(tx.getFromAccountId(), tx.getAmount());
            log.info("[TX-COMPLETE] ✅ Withdraw successful");
        }

        // Step 2b: Deposit to destination account
        if (tx.getToAccountId() != null && tx.getType() == TransactionType.TRANSFER) {
            log.info("[TX-COMPLETE] Depositing {} to accountId={} (TRANSFER)", tx.getAmount(), tx.getToAccountId());
            callDeposit(tx.getToAccountId(), tx.getAmount());
            log.info("[TX-COMPLETE] ✅ Deposit to destination successful");
        }
        if (tx.getType() == TransactionType.DEPOSIT && tx.getToAccountId() != null) {
            log.info("[TX-COMPLETE] Depositing {} to accountId={} (DEPOSIT)", tx.getAmount(), tx.getToAccountId());
            callDeposit(tx.getToAccountId(), tx.getAmount());
            log.info("[TX-COMPLETE] ✅ Deposit successful");
        }

        // Step 2c: Update status
        tx.setStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(tx);
        log.info("[TX-COMPLETE] Status updated to COMPLETED");

        // Step 2d: Publish completion event
        try {
            eventPublisher.publishTransactionCompleted(saved);
            log.info("[TX-COMPLETE] ✅ Completion event published");
        } catch (Exception e) {
            log.error("[TX-COMPLETE] ❌ Failed to publish completion event: {}", e.getMessage());
        }

        log.info("[TX-COMPLETE] ==================== TRANSACTION COMPLETED ====================\n");
        return transactionMapper.toDto(saved);
    }

    /**
     * Mark transaction as FAILED (does not reverse balances)
     */
    public TransactionResponseDto failTransaction(Long id, String reason) {
        log.info("[TX-FAIL] Marking transaction id={} as FAILED. Reason: {}", id, reason);
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        tx.setStatus(TransactionStatus.FAILED);
        tx.setDescription(tx.getDescription() + " | Failed: " + reason);
        Transaction saved = transactionRepository.save(tx);
        log.info("[TX-FAIL] ✅ Transaction id={} marked as FAILED", saved.getId());
        return transactionMapper.toDto(saved);
    }

    /**
     * Step 3: Reverse a COMPLETED transaction
     * - Restores balances (opposite of complete)
     * - Status becomes REVERSED
     */
    public TransactionResponseDto reverseTransaction(Long id) {
        log.info("[TX-REVERSE] ==================== REVERSING TRANSACTION id={} ====================", id);

        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("[TX-REVERSE] ❌ Transaction not found with id={}", id);
                    return new ResourceNotFoundException("Transaction", id);
                });

        if (tx.getStatus() != TransactionStatus.COMPLETED) {
            log.error("[TX-REVERSE] ❌ Cannot reverse. Status={} (must be COMPLETED)", tx.getStatus());
            throw new IllegalStateException("Only completed transactions can be reversed. Current status: " + tx.getStatus());
        }

        log.info("[TX-REVERSE] Reversing balances for trackingCode={}", tx.getTrackingCode());

        // Reverse: deposit back to source, withdraw from destination
        if (tx.getFromAccountId() != null &&
            (tx.getType() == TransactionType.TRANSFER || tx.getType() == TransactionType.WITHDRAWAL || tx.getType() == TransactionType.CARD_PAYMENT)) {
            log.info("[TX-REVERSE] Restoring {} to source accountId={}", tx.getAmount(), tx.getFromAccountId());
            callDeposit(tx.getFromAccountId(), tx.getAmount());
        }
        if (tx.getToAccountId() != null && tx.getType() == TransactionType.TRANSFER) {
            log.info("[TX-REVERSE] Reclaiming {} from destination accountId={}", tx.getAmount(), tx.getToAccountId());
            callWithdraw(tx.getToAccountId(), tx.getAmount());
        }
        if (tx.getType() == TransactionType.DEPOSIT && tx.getToAccountId() != null) {
            log.info("[TX-REVERSE] Reclaiming {} from accountId={}", tx.getAmount(), tx.getToAccountId());
            callWithdraw(tx.getToAccountId(), tx.getAmount());
        }

        tx.setStatus(TransactionStatus.REVERSED);
        Transaction saved = transactionRepository.save(tx);
        log.info("[TX-REVERSE] ✅ Transaction id={} reversed successfully\n", saved.getId());
        return transactionMapper.toDto(saved);
    }

    // ==================== INTERNAL HELPERS ====================

    private void callDeposit(Long accountId, BigDecimal amount) {
        String url = monolithBaseUrl + "/internal/accounts/" + accountId + "/deposit?amount=" + amount;
        log.info("[TX-REST] POST {}", url);
        try {
            restTemplate.postForEntity(url, null, String.class);
            log.info("[TX-REST] ✅ Monolith deposit OK for accountId={}", accountId);
        } catch (Exception e) {
            log.error("[TX-REST] ❌ Monolith deposit FAILED for accountId={}. Error: {}", accountId, e.getMessage());
            log.error("[TX-REST] ❌ Is Monolith running on {}? Check if port 8081 is active!", monolithBaseUrl);
            throw new IllegalStateException("Failed to update account balance during deposit. Monolith may be down. URL: " + url);
        }
    }

    private void callWithdraw(Long accountId, BigDecimal amount) {
        String url = monolithBaseUrl + "/internal/accounts/" + accountId + "/withdraw?amount=" + amount;
        log.info("[TX-REST] POST {}", url);
        try {
            restTemplate.postForEntity(url, null, String.class);
            log.info("[TX-REST] ✅ Monolith withdraw OK for accountId={}", accountId);
        } catch (Exception e) {
            log.error("[TX-REST] ❌ Monolith withdraw FAILED for accountId={}. Error: {}", accountId, e.getMessage());
            log.error("[TX-REST] ❌ Is Monolith running on {}? Check if port 8081 is active!", monolithBaseUrl);
            throw new IllegalStateException("Failed to update account balance during withdraw. Monolith may be down. URL: " + url);
        }
    }

    public TransactionResponseDto getById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private BigDecimal checkBalance(Long accountId) {
        String url = monolithBaseUrl + "/internal/accounts/" + accountId + "/balance";
        log.info("[TX-REST] GET {}", url);
        try {
            var response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && response.get("balance") != null) {
                BigDecimal balance = new BigDecimal(response.get("balance").toString());
                log.info("[TX-REST] ✅ Balance retrieved: {} for accountId={}", balance, accountId);
                return balance;
            }
            log.warn("[TX-REST] ⚠️ Balance response empty for accountId={}, returning ZERO", accountId);
        } catch (Exception e) {
            log.error("[TX-REST] ❌ Balance check FAILED for accountId={}. Error: {}", accountId, e.getMessage());
            log.error("[TX-REST] ❌ URL: {} — Is Monolith (8081) running?", url);
            throw new IllegalStateException("Cannot verify account balance. Monolith may be down. URL: " + url);
        }
        return BigDecimal.ZERO;
    }

    private String generateTrackingCode() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
