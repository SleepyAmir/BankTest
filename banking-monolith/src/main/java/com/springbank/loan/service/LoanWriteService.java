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
        Loan loan = loanMapper.toEntity(dto);
        loan.setStatus(LoanStatus.PENDING);
        loan.setInterestRate(creditScoreService.getRecommendedInterestRate(dto.userId()));
        loan.setMonthlyInstallment(loan.calculateMonthlyInstallment());
        Loan saved = loanRepository.save(loan);
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

    @Auditable(action = "APPROVE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public LoanResponseDto approveLoan(Long id, String approvedBy) {
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovedAt(LocalDateTime.now());
        loan.setApprovedBy(approvedBy);
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        loan.setRemainingAmount(loan.getAmount());

        // Generate installments
        List<LoanInstallment> installments = generateInstallments(loan);
        loan.setInstallments(installments);
        installmentRepository.saveAll(installments);

        Loan saved = loanRepository.save(loan);

        // Publish event
        var event = com.springbank.common.event.LoanApprovedEvent.builder()
                .loanId(saved.getId())
                .userId(saved.getUser().getId())
                .accountId(saved.getAccount().getId())
                .amount(saved.getAmount())
                .approvedBy(approvedBy)
                .approvedAt(saved.getApprovedAt())
                .build();
        rabbitTemplate.convertAndSend(EXCHANGE, "loan.approved", event);

        return loanMapper.toDto(saved);
    }

    @Auditable(action = "REJECT_LOAN", entity = "Loan")
    public LoanResponseDto rejectLoan(Long id, String reason) {
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        return loanMapper.toDto(loanRepository.save(loan));
    }

    @Auditable(action = "DELETE_LOAN", entity = "Loan")
    @CacheEvict(value = "loans", key = "#id")
    public void deleteLoan(Long id) {
        loanRepository.softDelete(id);
    }

    private List<LoanInstallment> generateInstallments(Loan loan) {
        List<LoanInstallment> list = new ArrayList<>();
        BigDecimal installmentAmount = loan.getMonthlyInstallment();
        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            LoanInstallment inst = LoanInstallment.builder()
                    .installmentNumber(i)
                    .amount(installmentAmount)
                    .dueDate(loan.getStartDate().plusMonths(i))
                    .status(com.springbank.common.enums.InstallmentStatus.PENDING)
                    .loan(loan)
                    .build();
            list.add(inst);
        }
        return list;
    }
}
