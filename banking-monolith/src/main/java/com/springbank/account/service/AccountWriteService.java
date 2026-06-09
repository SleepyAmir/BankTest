package com.springbank.account.service;

import com.springbank.account.dto.AccountCreateDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.dto.AccountUpdateDto;
import com.springbank.account.dto.AccountWithCardDto;
import com.springbank.account.dto.OpenAccountDto;
import com.springbank.account.entity.Account;
import com.springbank.account.entity.Branch;
import com.springbank.account.mapper.AccountMapper;
import com.springbank.account.repository.AccountRepository;
import com.springbank.account.repository.BranchRepository;
import com.springbank.card.dto.IssuedCardDto;
import com.springbank.card.service.CardWriteService;
import com.springbank.common.annotation.Auditable;
import com.springbank.common.exception.BusinessException;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.common.enums.AccountStatus;
import com.springbank.common.util.AccountNumberGenerator;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.entity.User;
import com.springbank.user.repository.KycVerificationRepository;
import com.springbank.user.repository.UserRepository;
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
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final KycVerificationRepository kycRepository;
    private final CardWriteService cardWriteService;

    public static final String EXCHANGE = "banking.exchange";

    /**
     * افتتاح حساب توسط کاربر و صدور خودکار کارت مجازی (فلوی ۳).
     * <p>
     * مراحل (همگی در یک تراکنش):
     *  ۱. بررسی وجود کاربر و تأیید بودن KYC او (پیش‌نیاز بانکی).
     *  ۲. یافتن شعبه با {@code branchCode}.
     *  ۳. ساخت حساب با شماره‌ی یکتا، موجودی صفر و وضعیت ACTIVE.
     *  ۴. انتشار AccountCreatedEvent.
     *  ۵. صدور بلافاصله‌ی کارت مجازی فعال برای حساب.
     */
    @Auditable(action = "OPEN_ACCOUNT", entity = "Account")
    public AccountWithCardDto openAccount(OpenAccountDto dto) {
        log.info("[ACCT-OPEN] درخواست افتتاح حساب: userId={}, type={}, branchCode={}",
                dto.userId(), dto.type(), dto.branchCode());

        User user = userRepository.findActiveById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.userId()));

        // پیش‌نیاز بانکی: KYC کاربر باید تأیید شده باشد
        KycVerification kyc = kycRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(
                        "برای افتتاح حساب ابتدا باید احراز هویت (KYC) را تکمیل و تأیید کنید"));
        if (!kyc.isApproved()) {
            throw new BusinessException("احراز هویت شما هنوز تأیید نشده است. وضعیت فعلی: " + kyc.getStatus());
        }

        Branch branch = branchRepository.findByCode(dto.branchCode())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.branchCode()));

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .type(dto.type())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .alias(dto.alias())
                // سقف انتقال از سطح KYC تعیین می‌شود
                .dailyTransferLimit(kyc.getDailyTransferLimit())
                .monthlyTransferLimit(kyc.getMonthlyTransferLimit())
                .user(user)
                .branch(branch)
                .build();

        Account saved = accountRepository.save(account);
        log.info("[ACCT-OPEN] ✅ حساب ساخته شد: id={}, number={}", saved.getId(), saved.getAccountNumber());

        publishAccountCreated(saved);

        // صدور بلافاصله‌ی کارت مجازی
        IssuedCardDto card = cardWriteService.issueVirtualCardForAccount(saved);

        return new AccountWithCardDto(accountMapper.toDto(saved), card);
    }

    private String generateUniqueAccountNumber() {
        String number;
        int attempts = 0;
        do {
            number = AccountNumberGenerator.generate();
            if (++attempts > 10) {
                throw new IllegalStateException("امکان تولید شماره حساب یکتا فراهم نشد");
            }
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private void publishAccountCreated(Account saved) {
        try {
            var event = com.springbank.common.event.AccountCreatedEvent.builder()
                    .accountId(saved.getId())
                    .accountNumber(saved.getAccountNumber())
                    .userId(saved.getUser().getId())
                    .initialBalance(saved.getBalance())
                    .build();
            rabbitTemplate.convertAndSend(EXCHANGE, "account.created", event);
            log.info("[ACCT-OPEN] ✅ AccountCreatedEvent published");
        } catch (Exception e) {
            log.warn("[ACCT-OPEN] ⚠️ انتشار AccountCreatedEvent ناموفق: {}", e.getMessage());
        }
    }

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
