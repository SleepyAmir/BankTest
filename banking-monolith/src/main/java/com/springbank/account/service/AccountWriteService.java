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
        if (accountRepository.existsByAccountNumber(dto.accountNumber())) {
            throw new IllegalArgumentException("Account number already exists: " + dto.accountNumber());
        }
        Account account = accountMapper.toEntity(dto);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);
        log.info("Account created: {}", saved.getId());
        return accountMapper.toDto(saved);
    }

    @Auditable(action = "UPDATE_ACCOUNT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponseDto updateAccount(Long id, AccountUpdateDto dto) {
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        accountMapper.updateFromDto(dto, account);
        Account saved = accountRepository.save(account);
        return accountMapper.toDto(saved);
    }

    @Auditable(action = "DELETE_ACCOUNT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public void deleteAccount(Long id) {
        accountRepository.softDelete(id);
        log.info("Account soft-deleted: {}", id);
    }

    @Auditable(action = "DEPOSIT", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponseDto deposit(Long id, BigDecimal amount) {
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.deposit(amount);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Auditable(action = "WITHDRAW", entity = "Account")
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponseDto withdraw(Long id, BigDecimal amount) {
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.withdraw(amount);
        return accountMapper.toDto(accountRepository.save(account));
    }
}
