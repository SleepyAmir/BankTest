package com.springbank.loan.service;

import com.springbank.common.enums.InstallmentStatus;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.loan.dto.LoanInstallmentDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.entity.Loan;
import com.springbank.loan.entity.LoanInstallment;
import com.springbank.loan.mapper.LoanMapper;
import com.springbank.loan.repository.LoanInstallmentRepository;
import com.springbank.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanReadService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanMapper loanMapper;

    public LoanResponseDto getById(Long id) {
        return loanRepository.findActiveById(id)
                .map(loanMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    public List<LoanResponseDto> getAll() {
        return loanRepository.findAllActive().stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<LoanResponseDto> getByUserId(Long userId) {
        return loanRepository.findByUserId(userId).stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    public Loan getEntityById(Long id) {
        return loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    public List<LoanInstallmentDto> getInstallmentsByLoanId(Long loanId) {
        return installmentRepository.findByLoanId(loanId).stream()
                .map(this::toInstallmentDto)
                .collect(Collectors.toList());
    }

    public List<LoanInstallmentDto> getPendingInstallments(Long loanId) {
        return installmentRepository.findByLoanIdAndStatus(loanId, InstallmentStatus.PENDING).stream()
                .map(this::toInstallmentDto)
                .collect(Collectors.toList());
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
