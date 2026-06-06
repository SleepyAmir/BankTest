package com.springbank.account.service;

import com.springbank.account.dto.AccountCreateDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.dto.AccountUpdateDto;
import com.springbank.account.entity.Account;
import com.springbank.account.mapper.AccountMapper;
import com.springbank.account.repository.AccountRepository;
import com.springbank.common.annotation.Auditable;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.common.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * ============================================================================
 * ACCOUNT WRITE SERVICE — Core Banking Operations
 * ============================================================================
 * Handles: Create Account, Update, Delete (soft), Deposit, Withdraw
 * Events: Publishes AccountCreatedEvent to RabbitMQ on creation
 *
 * LOG MARKERS: [ACCT-CREATE] [ACCT-UPDATE] [ACCT-DELETE] [ACCT-DEPOSIT] [ACCT-WITHDRAW]
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountWriteService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountReadService accountReadService;
    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "banking.exchange";

    @Auditable(action = "CREATE_ACCOUNT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#result.id")
    public AccountResponseDto createAccount(AccountCreateDto dto) {
        log.info("[ACCT-CREATE] Creating new account for userId={}, accountNumber={}", dto.userId(), dto.accountNumber());

        if (accountRepository.existsByAccountNumber(dto.accountNumber())) {
            log.error("[ACCT-CREATE] ❌ Account number already exists: {}", dto.accountNumber());
            throw new IllegalArgumentException("Account number already exists: " + dto.accountNumber());
        }

        Account account = accountMapper.toEntity(dto);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);
        log.info("[ACCT-CREATE] ✅ Account created: id={}, accountNumber={}, balance={}",
                saved.getId(), saved.getAccountNumber(), saved.getBalance());

        // Publish event for downstream services
        try {
            var event = com.springbank.common.event.AccountCreatedEvent.builder()
                    .accountId(saved.getId())
                    .accountNumber(saved.getAccountNumber())
                    .userId(saved.getUser().getId())
                    .initialBalance(saved.getBalance())
                    .build();
            rabbitTemplate.convertAndSend(EXCHANGE, "account.created", event);
            log.info("[ACCT-CREATE] ✅ AccountCreatedEvent published to RabbitMQ");
        } catch (Exception e) {
            log.warn("[ACCT-CREATE] ⚠️ Failed to publish AccountCreatedEvent: {}. Account was saved but event not sent.", e.getMessage());
        }

        return accountMapper.toDto(saved);
    }

    @Auditable(action = "UPDATE_ACCOUNT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponseDto updateAccount(Long id, AccountUpdateDto dto) {
        log.info("[ACCT-UPDATE] Updating account id={}", id);
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[ACCT-UPDATE] ❌ Account not found with id={}", id);
                    return new ResourceNotFoundException("Account", id);
                });
        accountMapper.updateFromDto(dto, account);
        Account saved = accountRepository.save(account);
        log.info("[ACCT-UPDATE] ✅ Account id={} updated successfully", saved.getId());
        return accountMapper.toDto(saved);
    }

    @Auditable(action = "DELETE_ACCOUNT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public void deleteAccount(Long id) {
        log.info("[ACCT-DELETE] Soft-deleting account id={}", id);
        accountRepository.softDelete(id);
        log.info("[ACCT-DELETE] ✅ Account id={} soft-deleted", id);
    }

    @Auditable(action = "DEPOSIT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponseDto deposit(Long id, BigDecimal amount) {
        log.info("[ACCT-DEPOSIT] Depositing {} to account id={}", amount, id);
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[ACCT-DEPOSIT] ❌ Account not found with id={}", id);
                    return new ResourceNotFoundException("Account", id);
                });
        BigDecimal oldBalance = account.getBalance();
        account.deposit(amount);
        Account saved = accountRepository.save(account);
        log.info("[ACCT-DEPOSIT] ✅ Deposited {} to account id={}. Balance: {} → {}",
                amount, id, oldBalance, saved.getBalance());
        return accountMapper.toDto(saved);
    }

    @Auditable(action = "WITHDRAW", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponseDto withdraw(Long id, BigDecimal amount) {
        log.info("[ACCT-WITHDRAW] Withdrawing {} from account id={}", amount, id);
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[ACCT-WITHDRAW] ❌ Account not found with id={}", id);
                    return new ResourceNotFoundException("Account", id);
                });
        BigDecimal oldBalance = account.getBalance();
        account.withdraw(amount);
        Account saved = accountRepository.save(account);
        log.info("[ACCT-WITHDRAW] ✅ Withdrew {} from account id={}. Balance: {} → {}",
                amount, id, oldBalance, saved.getBalance());
        return accountMapper.toDto(saved);
    }
}
