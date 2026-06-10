package com.springbank.loan.service;

import com.springbank.account.entity.Account;
import com.springbank.account.repository.AccountRepository;
import com.springbank.common.annotation.Auditable;
import com.springbank.common.enums.InstallmentStatus;
import com.springbank.common.enums.LoanStatus;
import com.springbank.common.enums.NotificationType;
import com.springbank.common.enums.TransactionType;
import com.springbank.common.exception.BusinessException;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.loan.dto.LoanCreateDto;
import com.springbank.loan.dto.LoanInstallmentDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.dto.LoanUpdateDto;
import com.springbank.loan.entity.CreditScore;
import com.springbank.loan.entity.Loan;
import com.springbank.loan.entity.LoanInstallment;
import com.springbank.loan.mapper.LoanMapper;
import com.springbank.loan.repository.CreditScoreRepository;
import com.springbank.loan.repository.LoanInstallmentRepository;
import com.springbank.loan.repository.LoanRepository;
import com.springbank.notification.service.NotificationService;
import com.springbank.user.entity.User;
import com.springbank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * LOAN WRITE SERVICE — درخواست، تأیید، واریز و بازپرداخت وام (فلوهای ۹ و ۱۰)
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanWriteService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanMapper loanMapper;
    private final CreditScoreService creditScoreService;
    private final CreditScoreRepository creditScoreRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;
    private final com.springbank.loan.client.TransactionServiceClient transactionServiceClient;

    public static final String EXCHANGE = "banking.exchange";

    // ===================== فلوی ۹: درخواست وام =====================

    @Auditable(action = "CREATE_LOAN", entity = "Loan")
    public LoanResponseDto createLoan(LoanCreateDto dto) {
        log.info("[LOAN-CREATE] درخواست وام: userId={}, amount={}, duration={} ماه",
                dto.userId(), dto.amount(), dto.durationMonths());

        User user = userRepository.findActiveById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.userId()));
        Account account = accountRepository.findActiveById(dto.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", dto.accountId()));

        // اعتبارسنجی: حداکثر مبلغ مجاز بر اساس امتیاز اعتباری (getLoanMultiplier)
        BigDecimal maxAllowed = creditScoreService.getMaxAllowedLoanAmount(dto.userId());
        if (dto.amount().compareTo(maxAllowed) > 0) {
            throw new BusinessException(String.format(
                    "مبلغ درخواستی (%s) از حداکثر مجاز بر اساس امتیاز اعتباری شما (%s) بیشتر است",
                    dto.amount(), maxAllowed));
        }

        Loan loan = Loan.builder()
                .amount(dto.amount())
                .durationMonths(dto.durationMonths())
                .purpose(dto.purpose())
                .status(LoanStatus.PENDING)
                .user(user)
                .account(account)
                .build();

        // نرخ سود پیشنهادی و قسط ماهانه بر اساس امتیاز اعتباری
        loan.setInterestRate(creditScoreService.getRecommendedInterestRate(dto.userId()));
        loan.setMonthlyInstallment(loan.calculateMonthlyInstallment());

        // snapshot امتیاز اعتباری لحظه‌ی درخواست
        creditScoreRepository.findByUserId(dto.userId()).ifPresent(loan::setCreditScore);

        Loan saved = loanRepository.save(loan);
        log.info("[LOAN-CREATE] ✅ وام ثبت شد: id={}, status=PENDING, نرخ={}%, قسط ماهانه={}",
                saved.getId(), saved.getInterestRate(), saved.getMonthlyInstallment());
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "UPDATE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public LoanResponseDto updateLoan(Long id, LoanUpdateDto dto) {
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        loanMapper.updateFromDto(dto, loan);
        return loanMapper.toDto(loanRepository.save(loan));
    }

    // ===================== فلوی ۹: تأیید و واریز =====================

    @Auditable(action = "APPROVE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public LoanResponseDto approveLoan(Long id, String approvedBy) {
        log.info("[LOAN-APPROVE] تأیید وام id={} توسط {}", id, approvedBy);

        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("وام باید در وضعیت PENDING باشد. وضعیت فعلی: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovedAt(LocalDateTime.now());
        loan.setApprovedBy(approvedBy);
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        loan.setRemainingAmount(loan.getAmount());

        // تولید جدول اقساط
        List<LoanInstallment> installments = generateInstallments(loan);
        loan.setInstallments(installments);
        installmentRepository.saveAll(installments);
        log.info("[LOAN-APPROVE] ✅ {} قسط ساخته شد", installments.size());

        Loan saved = loanRepository.save(loan);

        // واریز مبلغ وام از طریق transaction-write (تنها مرجع تراکنش — جابجایی اتمیک + ثبت تراکنش)
        boolean disbursed = transactionServiceClient.createTransaction(
                TransactionType.LOAN_DISBURSEMENT, loan.getAmount(),
                null, loan.getAccount().getId(), loan.getUser().getId(),
                "loan", "Loan disbursement for loan #" + saved.getId());
        if (!disbursed) {
            // اگر واریز ناموفق بود، کل تأیید را برگردان (تراکنش DB rollback می‌شود)
            throw new IllegalStateException("واریز مبلغ وام ناموفق بود. تأیید لغو شد.");
        }
        log.info("[LOAN-APPROVE] ✅ مبلغ {} به حساب id={} واریز شد (از طریق transaction-write)",
                loan.getAmount(), loan.getAccount().getId());

        // رویداد تأیید وام برای نوتیفیکیشن
        publishLoanApproved(saved, approvedBy);

        log.info("[LOAN-APPROVE] ✅ وام id={} تأیید و واریز شد", saved.getId());
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "REJECT_LOAN", entity = "Loan")
    public LoanResponseDto rejectLoan(Long id, String reason) {
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        Loan saved = loanRepository.save(loan);
        notificationService.notifyInApp(loan.getUser(), NotificationType.LOAN_REJECTED,
                "درخواست وام رد شد", "درخواست وام شما رد شد. دلیل: " + reason, null);
        log.info("[LOAN-REJECT] ✅ وام id={} رد شد", saved.getId());
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "DELETE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public void deleteLoan(Long id) {
        loanRepository.softDelete(id);
    }

    // ===================== فلوی ۱۰: بازپرداخت قسط =====================

    @Auditable(action = "PAY_INSTALLMENT", entity = "LoanInstallment")
    public LoanInstallmentDto payInstallment(Long installmentId) {
        log.info("[LOAN-PAY] پرداخت قسط id={}", installmentId);

        LoanInstallment inst = installmentRepository.findActiveById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Installment", installmentId));

        if (inst.getStatus() == InstallmentStatus.PAID) {
            throw new IllegalStateException("این قسط قبلاً پرداخت شده است");
        }

        Loan loan = inst.getLoan();
        Account account = accountRepository.findActiveById(loan.getAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", loan.getAccount().getId()));

        // محاسبه‌ی جریمه‌ی دیرکرد (۲٪ ماهانه) و روزهای تأخیر
        LocalDate today = LocalDate.now();
        boolean late = today.isAfter(inst.getDueDate());
        BigDecimal lateFee = BigDecimal.ZERO;
        int daysOverdue = 0;
        if (late) {
            daysOverdue = (int) ChronoUnit.DAYS.between(inst.getDueDate(), today);
            BigDecimal dailyRate = new BigDecimal("0.02").divide(new BigDecimal("30"), 10, java.math.RoundingMode.HALF_UP);
            lateFee = inst.getAmount().multiply(dailyRate).multiply(BigDecimal.valueOf(daysOverdue))
                    .setScale(4, java.math.RoundingMode.HALF_UP);
        }
        inst.setLateFee(lateFee);
        inst.setDaysOverdue(daysOverdue);

        BigDecimal totalDue = inst.getAmount().add(lateFee);

        // پیش‌بررسی موجودی برای پیام بهتر (کسر واقعی توسط transaction-write انجام می‌شود)
        if (account.getBalance().compareTo(totalDue) < 0) {
            throw new BusinessException(String.format(
                    "موجودی حساب کافی نیست. مبلغ قسط + جریمه: %s، موجودی: %s", totalDue, account.getBalance()));
        }

        // کسر اتمیک از حساب از طریق transaction-write (تنها مرجع تراکنش)
        boolean paid = transactionServiceClient.createTransaction(
                TransactionType.LOAN_PAYMENT, totalDue,
                account.getId(), null, loan.getUser().getId(),
                "loan-installment", "Installment #" + inst.getInstallmentNumber() + " for loan #" + loan.getId());
        if (!paid) {
            throw new IllegalStateException("کسر مبلغ قسط از حساب ناموفق بود.");
        }

        // ثبت پرداخت
        inst.setStatus(InstallmentStatus.PAID);
        inst.setPaidDate(today);
        inst.setPaidAmount(totalDue);

        // کاهش مانده‌ی بدهی
        loan.setRemainingAmount(loan.getRemainingAmount().subtract(inst.getAmount()));
        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.COMPLETED);
            loan.setRemainingAmount(BigDecimal.ZERO);
            log.info("[LOAN-PAY] ✅ آخرین قسط وام id={} پرداخت شد → COMPLETED", loan.getId());
        }

        LoanInstallment savedInst = installmentRepository.save(inst);
        loanRepository.save(loan);

        // اثر بر امتیاز اعتباری (پرداخت به‌موقع مثبت، با تأخیر منفی)
        creditScoreService.recordInstallmentPayment(loan.getUser().getId(), !late);

        // نوتیفیکیشن
        String msg = late
                ? String.format("قسط شماره %d با تأخیر %d روزه و جریمه‌ی %s پرداخت شد.",
                    inst.getInstallmentNumber(), daysOverdue, lateFee)
                : String.format("قسط شماره %d با موفقیت و به‌موقع پرداخت شد.", inst.getInstallmentNumber());
        notificationService.notifyInApp(loan.getUser(), NotificationType.TRANSACTION_DONE,
                "پرداخت قسط", msg, null);

        log.info("[LOAN-PAY] ✅ قسط id={} پرداخت شد (late={}, fee={}). مانده وام={}",
                savedInst.getId(), late, lateFee, loan.getRemainingAmount());

        return toInstallmentDto(savedInst);
    }

    // ===================== Helpers =====================

    private List<LoanInstallment> generateInstallments(Loan loan) {
        List<LoanInstallment> list = new ArrayList<>();
        BigDecimal monthly = loan.getMonthlyInstallment();
        BigDecimal monthlyRate = loan.getInterestRate()
                .divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);

        BigDecimal remaining = loan.getAmount();
        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            BigDecimal interestPart = remaining.multiply(monthlyRate).setScale(4, java.math.RoundingMode.HALF_UP);
            BigDecimal principalPart = monthly.subtract(interestPart).setScale(4, java.math.RoundingMode.HALF_UP);
            remaining = remaining.subtract(principalPart);

            LoanInstallment inst = LoanInstallment.builder()
                    .installmentNumber(i)
                    .amount(monthly)
                    .principalPart(principalPart)
                    .interestPart(interestPart)
                    .dueDate(loan.getStartDate().plusMonths(i))
                    .status(InstallmentStatus.PENDING)
                    .lateFee(BigDecimal.ZERO)
                    .daysOverdue(0)
                    .loan(loan)
                    .build();
            list.add(inst);
        }
        return list;
    }

    private void publishLoanApproved(Loan saved, String approvedBy) {
        try {
            var event = com.springbank.common.event.LoanApprovedEvent.builder()
                    .loanId(saved.getId())
                    .userId(saved.getUser().getId())
                    .accountId(saved.getAccount().getId())
                    .amount(saved.getAmount())
                    .approvedBy(approvedBy)
                    .approvedAt(saved.getApprovedAt())
                    .build();
            rabbitTemplate.convertAndSend(EXCHANGE, "loan.approved", event);
        } catch (Exception e) {
            log.warn("[LOAN-APPROVE] ⚠️ انتشار LoanApprovedEvent ناموفق بود: {}", e.getMessage());
        }
    }

    private LoanInstallmentDto toInstallmentDto(LoanInstallment i) {
        return new LoanInstallmentDto(
                i.getId(),
                i.getLoan() != null ? i.getLoan().getId() : null,
                i.getInstallmentNumber(),
                i.getAmount(),
                i.getPrincipalPart(),
                i.getInterestPart(),
                i.getDueDate(),
                i.getPaidDate(),
                i.getStatus(),
                i.getLateFee(),
                i.getDaysOverdue()
        );
    }
}
