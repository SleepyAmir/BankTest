package com.springbank.config;

import com.springbank.account.entity.Account;
import com.springbank.account.repository.AccountRepository;
import com.springbank.card.entity.Card;
import com.springbank.card.repository.CardRepository;
import com.springbank.common.enums.*;
import com.springbank.loan.entity.CreditScore;
import com.springbank.loan.entity.Loan;
import com.springbank.loan.repository.CreditScoreRepository;
import com.springbank.loan.repository.LoanRepository;
import com.springbank.notification.entity.Notification;
import com.springbank.notification.repository.NotificationRepository;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.entity.Permission;
import com.springbank.user.entity.Role;
import com.springbank.user.entity.User;
import com.springbank.user.repository.KycVerificationRepository;
import com.springbank.user.repository.PermissionRepository;
import com.springbank.user.repository.RoleRepository;
import com.springbank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * ============================================================================
 * DATA INITIALIZER — Idempotent: هر entity فقط اگر وجود نداشت ساخته می‌شود
 * ============================================================================
 * NOTE: Collections must be modified in-place (not replaced with immutable Set.of)
 * to avoid Hibernate UnsupportedOperationException on merge.
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final LoanRepository loanRepository;
    private final NotificationRepository notificationRepository;
    private final KycVerificationRepository kycVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void init() {
        log.info("[INIT-START] ==================== SEEDING DATA (idempotent) ====================");

        // ======== PERMISSIONS (idempotent) ========
        Permission p1 = upsertPermission("READ_USERS", "Read users");
        Permission p2 = upsertPermission("WRITE_USERS", "Write users");
        Permission p3 = upsertPermission("READ_ACCOUNTS", "Read accounts");
        Permission p4 = upsertPermission("WRITE_ACCOUNTS", "Write accounts");
        log.info("[INIT-PERM] ✅ Permissions ready");

        // ======== ROLES (idempotent) ========
        Role adminRole = upsertRole("ADMIN", "System Administrator");
        Role customerRole = upsertRole("CUSTOMER", "Bank Customer");
        log.info("[INIT-ROLE] ✅ Roles ready: ADMIN id={}, CUSTOMER id={}", adminRole.getId(), customerRole.getId());

        // Assign permissions to ADMIN role (in-place, avoid replacing collection)
        if (adminRole.getPermissions() == null || adminRole.getPermissions().isEmpty()) {
            adminRole.getPermissions().addAll(Set.of(p1, p2, p3, p4));
            roleRepository.save(adminRole);
            log.info("[INIT-PERM] ✅ Permissions assigned to ADMIN");
        }

        // ======== USERS (idempotent) ========
        User admin = upsertUser("admin", "admin123", "admin@springbank.com", "System", "Admin", "09120000000", Set.of(adminRole));
        User customer = upsertUser("customer", "customer123", "customer@springbank.com", "Ali", "Ahmadi", "09121111111", Set.of(customerRole));
        log.info("[INIT-USER] ✅ Admin user id={}, Customer user id={}", admin.getId(), customer.getId());

        // ======== KYC (idempotent) ========
        upsertKyc(admin, KycStatus.APPROVED, KycLevel.ENHANCED, "system");
        upsertKyc(customer, KycStatus.PENDING, KycLevel.BASIC, null);
        log.info("[INIT-KYC] ✅ KYC ready");

        // ======== CREDIT SCORE (idempotent) ========
        CreditScore creditScore = upsertCreditScore(customer);
        log.info("[INIT-CREDIT] ✅ CreditScore id={}", creditScore.getId());

        // ======== ACCOUNT (idempotent) ========
        Account account = upsertAccount(customer, "IR123456789012345678901234");
        log.info("[INIT-ACCT] ✅ Account id={}", account.getId());

        // ======== CARD (idempotent) ========
        Card card = upsertCard(account, "6104331234567890");
        log.info("[INIT-CARD] ✅ Card id={}", card.getId());

        // ======== LOAN (idempotent) ========
        upsertLoan(customer, account, creditScore);
        log.info("[INIT-LOAN] ✅ Loan ready");

        // ======== NOTIFICATION (idempotent) ========
        upsertNotification(customer);
        log.info("[INIT-NOTIF] ✅ Notification ready");

        log.info("[INIT-DONE] ==================== DATA SEED COMPLETE ====================");
        log.info("[INIT-DONE] 🔑 Login as admin:    username=admin    password=admin123");
        log.info("[INIT-DONE] 🔑 Login as customer: username=customer password=customer123");
    }

    // ========================= HELPER METHODS =========================

    private Permission upsertPermission(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("[INIT-PERM] Creating permission: {}", name);
                    Permission p = Permission.builder().name(name).description(description).build();
                    return permissionRepository.save(p);
                });
    }

    private Role upsertRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("[INIT-ROLE] Creating role: {}", name);
                    Role r = Role.builder().name(name).description(description).build();
                    return roleRepository.save(r);
                });
    }

    private User upsertUser(String username, String rawPassword, String email, String firstName, String lastName, String phone, Set<Role> roles) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    log.info("[INIT-USER] Creating user: {}", username);
                    User u = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(rawPassword))
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .phoneNumber(phone)
                            .enabled(true)
                            .emailVerified(true)
                            .build();
                    // Add roles in-place (avoid immutable Set.of in builder for managed collections)
                    u.getRoles().addAll(roles);
                    return userRepository.save(u);
                });
    }

    private void upsertKyc(User user, KycStatus status, KycLevel level, String verifiedBy) {
        if (!kycVerificationRepository.existsByUserId(user.getId())) {
            log.info("[INIT-KYC] Creating KYC for user: {}", user.getUsername());
            KycVerification kyc = KycVerification.builder()
                    .user(user)
                    .status(status)
                    .level(level)
                    .verifiedBy(verifiedBy)
                    .verifiedAt(verifiedBy != null ? LocalDateTime.now() : null)
                    .build();
            kycVerificationRepository.save(kyc);
        }
    }

    private CreditScore upsertCreditScore(User user) {
        return creditScoreRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("[INIT-CREDIT] Creating credit score for user: {}", user.getUsername());
                    CreditScore cs = CreditScore.builder()
                            .score(750)
                            .grade(CreditGrade.VERY_GOOD)
                            .paymentHistoryScore(new BigDecimal("90.0"))
                            .creditUtilizationScore(new BigDecimal("30.0"))
                            .accountAgeScore(new BigDecimal("80.0"))
                            .creditMixScore(new BigDecimal("70.0"))
                            .newCreditScore(new BigDecimal("50.0"))
                            .calculatedAt(LocalDateTime.now())
                            .nextRecalculationAt(LocalDateTime.now().plusDays(30))
                            .user(user)
                            .build();
                    return creditScoreRepository.save(cs);
                });
    }

    private Account upsertAccount(User user, String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> {
                    log.info("[INIT-ACCT] Creating account: {}", accountNumber);
                    Account a = Account.builder()
                            .accountNumber(accountNumber)
                            .type(AccountType.CHECKING)
                            .balance(new BigDecimal("100000000"))
                            .status(AccountStatus.ACTIVE)
                            .alias("Main Account")
                            .dailyTransferLimit(new BigDecimal("50000000"))
                            .monthlyTransferLimit(new BigDecimal("200000000"))
                            .user(user)
                            .build();
                    return accountRepository.save(a);
                });
    }

    private Card upsertCard(Account account, String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseGet(() -> {
                    log.info("[INIT-CARD] Creating card: {}", cardNumber);
                    Card c = Card.builder()
                            .cardNumber(cardNumber)
                            .cvv2("123")
                            .pin(passwordEncoder.encode("1234"))
                            .expiryDate(LocalDate.of(2028, 12, 31))
                            .status(CardStatus.ACTIVE)
                            .type(CardType.DEBIT)
                            .dailyLimit(new BigDecimal("10000000"))
                            .account(account)
                            .build();
                    return cardRepository.save(c);
                });
    }

    private void upsertLoan(User user, Account account, CreditScore creditScore) {
        if (loanRepository.findByUserId(user.getId()).isEmpty()) {
            log.info("[INIT-LOAN] Creating loan for user: {}", user.getUsername());
            Loan loan = Loan.builder()
                    .amount(new BigDecimal("1000000000"))
                    .interestRate(new BigDecimal("14.0"))
                    .durationMonths(12)
                    .monthlyInstallment(new BigDecimal("90258338"))
                    .status(LoanStatus.PENDING)
                    .purpose("House Renovation")
                    .user(user)
                    .account(account)
                    .creditScore(creditScore)
                    .build();
            loanRepository.save(loan);
        }
    }

    private void upsertNotification(User user) {
        if (notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) {
            log.info("[INIT-NOTIF] Creating notification for user: {}", user.getUsername());
            Notification notif = Notification.builder()
                    .type(NotificationType.LOAN_APPROVED)
                    .title("Welcome to SpringBank")
                    .message("Your account has been successfully created. Welcome!")
                    .isRead(false)
                    .channel(NotificationChannel.IN_APP)
                    .user(user)
                    .build();
            notificationRepository.save(notif);
        }
    }
}
