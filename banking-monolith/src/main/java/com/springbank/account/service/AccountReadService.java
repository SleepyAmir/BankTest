package com.springbank.account.service;

import com.springbank.account.dto.AccountBalanceDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.entity.Account;
import com.springbank.account.mapper.AccountMapper;
import com.springbank.account.repository.AccountRepository;
import com.springbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountReadService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Cacheable(value = "accounts", key = "#id")
    public AccountResponseDto getById(Long id) {
        return accountRepository.findActiveById(id)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    public List<AccountResponseDto> getByUserId(Long userId) {
        return accountRepository.findActiveByUserId(userId).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    public Account getEntityById(Long id) {
        return accountRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    @Cacheable(value = "account-balance", key = "#id")
    public AccountBalanceDto getBalanceInternal(Long id) {
        Account account = accountRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        return new AccountBalanceDto(account.getId(), account.getAccountNumber(), account.getBalance());
    }

    public BigDecimal getBalance(Long id) {
        return accountRepository.findBalanceById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    public List<AccountResponseDto> getAllActive() {
        return accountRepository.findAllActive().stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }
}
