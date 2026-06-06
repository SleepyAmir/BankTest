package com.springbank.loan.scheduler;

import com.springbank.loan.entity.LoanInstallment;
import com.springbank.loan.repository.LoanInstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstallmentReminderJob {

    private final LoanInstallmentRepository installmentRepository;

    @Scheduled(cron = "0 0 9 * * ?") // Every day at 9:00 AM
    public void remindOverdueInstallments() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<LoanInstallment> upcoming = installmentRepository.findByLoanIdAndStatus(null, com.springbank.common.enums.InstallmentStatus.PENDING);
        // In a real implementation, filter by dueDate = tomorrow
        log.info("Installment reminder job executed. {} installments upcoming.", upcoming.size());
    }
}
