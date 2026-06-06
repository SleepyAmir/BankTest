package com.springbank.loan.service;

import com.springbank.common.annotation.Auditable;
import com.springbank.common.enums.LoanStatus;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.loan.dto.LoanCreateDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.dto.LoanUpdateDto;
import com.springbank.loan.entity.Loan;
import com.springbank.loan.mapper.LoanMapper;
import com.springbank.loan.repository.LoanRepository;
import com.springbank.loan.repository.LoanInstallmentRepository;
import com.springbank.loan.entity.LoanInstallment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * LOAN WRITE SERVICE
 * ============================================================================
 * Flow: Create Loan (PENDING) → Approve (ACTIVE + installments) → Reject
 * Events: LoanApprovedEvent published to RabbitMQ on approval
 *
 * LOG MARKERS: [LOAN-CREATE] [LOAN-UPDATE] [LOAN-APPROVE] [LOAN-REJECT] [LOAN-DELETE]
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
    private final LoanReadService loanReadService;
    private final CreditScoreService creditScoreService;
    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "banking.exchange";

    @Auditable(action = "CREATE_LOAN", entity = "Loan")
    public LoanResponseDto createLoan(LoanCreateDto dto) {
        log.info("[LOAN-CREATE] Creating loan request: userId={}, amount={}, duration={} months",
                dto.userId(), dto.amount(), dto.durationMonths());

        Loan loan = loanMapper.toEntity(dto);
        loan.setStatus(LoanStatus.PENDING);

        BigDecimal interestRate = creditScoreService.getRecommendedInterestRate(dto.userId());
        loan.setInterestRate(interestRate);
        log.info("[LOAN-CREATE] Credit score based interest rate: {}%", interestRate);

        BigDecimal monthly = loan.calculateMonthlyInstallment();
        loan.setMonthlyInstallment(monthly);
        log.info("[LOAN-CREATE] Calculated monthly installment: {}", monthly);

        Loan saved = loanRepository.save(loan);
        log.info("[LOAN-CREATE] ✅ Loan created: id={}, status={}, monthlyInstallment={}",
                saved.getId(), saved.getStatus(), saved.getMonthlyInstallment());
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "UPDATE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public LoanResponseDto updateLoan(Long id, LoanUpdateDto dto) {
        log.info("[LOAN-UPDATE] Updating loan id={}", id);
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[LOAN-UPDATE] ❌ Loan not found with id={}", id);
                    return new ResourceNotFoundException("Loan", id);
                });
        loanMapper.updateFromDto(dto, loan);
        Loan saved = loanRepository.save(loan);
        log.info("[LOAN-UPDATE] ✅ Loan id={} updated", saved.getId());
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "APPROVE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public LoanResponseDto approveLoan(Long id, String approvedBy) {
        log.info("[LOAN-APPROVE] ==================== APPROVING LOAN id={} by {} ====================", id, approvedBy);

        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[LOAN-APPROVE] ❌ Loan not found with id={}", id);
                    return new ResourceNotFoundException("Loan", id);
                });

        if (loan.getStatus() != LoanStatus.PENDING) {
            log.error("[LOAN-APPROVE] ❌ Cannot approve loan. Current status={} (expected PENDING)", loan.getStatus());
            throw new IllegalStateException("Loan must be in PENDING status to approve. Current: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovedAt(LocalDateTime.now());
        loan.setApprovedBy(approvedBy);
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        loan.setRemainingAmount(loan.getAmount());

        // Generate installment schedule
        log.info("[LOAN-APPROVE] Generating {} monthly installments...", loan.getDurationMonths());
        List<LoanInstallment> installments = generateInstallments(loan);
        loan.setInstallments(installments);
        installmentRepository.saveAll(installments);
        log.info("[LOAN-APPROVE] ✅ {} installments saved to database", installments.size());

        Loan saved = loanRepository.save(loan);
        log.info("[LOAN-APPROVE] ✅ Loan approved: id={}, startDate={}, endDate={}, monthlyInstallment={}",
                saved.getId(), saved.getStartDate(), saved.getEndDate(), saved.getMonthlyInstallment());

        // Publish event to RabbitMQ for notifications and other services
        try {
            var event = com.springbank.common.event.LoanApprovedEvent.builder()
                    .loanId(saved.getId())
                    .userId(saved.getUser().getId())
                    .accountId(saved.getAccount().getId())
                    .amount(saved.getAmount())
                    .approvedBy(approvedBy)
                    .approvedAt(saved.getApprovedAt())
                    .build();
            log.info("[LOAN-APPROVE] Publishing LoanApprovedEvent to RabbitMQ (userId={}, loanId={})...",
                    event.getUserId(), event.getLoanId());
            rabbitTemplate.convertAndSend(EXCHANGE, "loan.approved", event);
            log.info("[LOAN-APPROVE] ✅ LoanApprovedEvent published. Notification service will send notification to user.");
        } catch (Exception e) {
            log.error("[LOAN-APPROVE] ❌ Failed to publish LoanApprovedEvent: {}. Loan approved but user won't get notification!", e.getMessage());
        }

        log.info("[LOAN-APPROVE] ==================== LOAN APPROVED SUCCESSFULLY ====================\n");
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "REJECT_LOAN", entity = "Loan")
    public LoanResponseDto rejectLoan(Long id, String reason) {
        log.info("[LOAN-REJECT] Rejecting loan id={}. Reason: {}", id, reason);
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[LOAN-REJECT] ❌ Loan not found with id={}", id);
                    return new ResourceNotFoundException("Loan", id);
                });
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        Loan saved = loanRepository.save(loan);
        log.info("[LOAN-REJECT] ✅ Loan id={} rejected. Reason: {}", saved.getId(), reason);
        return loanMapper.toDto(saved);
    }

    @Auditable(action = "DELETE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public void deleteLoan(Long id) {
        log.info("[LOAN-DELETE] Soft-deleting loan id={}", id);
        loanRepository.softDelete(id);
        log.info("[LOAN-DELETE] ✅ Loan id={} soft-deleted", id);
    }

    /**
     * Generate monthly installment schedule using PMT formula
     */
    private List<LoanInstallment> generateInstallments(Loan loan) {
        List<LoanInstallment> list = new ArrayList<>();
        BigDecimal installmentAmount = loan.getMonthlyInstallment();
        log.info("[LOAN-INSTALL] Each installment amount={} for {} months", installmentAmount, loan.getDurationMonths());

        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            LoanInstallment inst = LoanInstallment.builder()
                    .installmentNumber(i)
                    .amount(installmentAmount)
                    .dueDate(loan.getStartDate().plusMonths(i))
                    .status(com.springbank.common.enums.InstallmentStatus.PENDING)
                    .loan(loan)
                    .build();
            list.add(inst);
            log.debug("[LOAN-INSTALL] Installment #{} dueDate={}", i, inst.getDueDate());
        }
        return list;
    }
}
