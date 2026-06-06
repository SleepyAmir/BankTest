package com.springbank.loan.repository;

import com.springbank.common.enums.InstallmentStatus;
import com.springbank.loan.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {
    List<LoanInstallment> findByLoanId(Long loanId);
    List<LoanInstallment> findByLoanIdAndStatus(Long loanId, InstallmentStatus status);
}
