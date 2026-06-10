package com.springbank.account.service;

import com.springbank.account.dto.AccountBalanceDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.entity.Account;
import com.springbank.account.mapper.AccountMapper;
import com.springbank.account.repository.AccountRepository;
import com.springbank.card.repository.CardRepository;
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
    private final CardRepository cardRepository;

    @Cacheable(value = "accounts", key = "#id")
    public AccountResponseDto getById(Long id) {
        return accountRepository.findActiveById(id)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    /** یافتن حساب با شماره‌ی حساب (IR...) — برای انتقال وجه با شماره حساب. */
    public AccountResponseDto getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
    }

    /**
     * یافتن حساب مقصد با «شماره‌ی حساب (IR...)» یا «شماره‌ی کارت ۱۶ رقمی».
     * اگر ورودی ۱۶ رقم باشد به‌عنوان شماره‌ی کارت در نظر گرفته می‌شود؛ در غیر این صورت شماره‌ی حساب.
     * فقط اطلاعات حداقلی برمی‌گرداند (بدون افشای موجودی).
     */
    public com.springbank.account.dto.AccountLookupDto lookupByAccountNumber(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Account", "");
        }
        String value = identifier.trim().replace("-", "").replace(" ", "");

        Account account;
        if (value.matches("\\d{16}")) {
            // شماره‌ی کارت → حساب مرتبط
            var card = cardRepository.findByCardNumber(value)
                    .orElseThrow(() -> new ResourceNotFoundException("Card", value));
            account = card.getAccount();
            if (account == null) {
                throw new ResourceNotFoundException("Account for card", value);
            }
        } else {
            // شماره‌ی حساب (IR...)
            account = accountRepository.findByAccountNumber(value)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", value));
        }

        String ownerName = account.getUser() != null ? account.getUser().getFullName() : "";
        return new com.springbank.account.dto.AccountLookupDto(
                account.getId(), account.getAccountNumber(), ownerName, account.getStatus().name());
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
