package com.springbank.loan.scheduler;

import com.springbank.common.enums.InstallmentStatus;
import com.springbank.common.enums.NotificationChannel;
import com.springbank.common.enums.NotificationType;
import com.springbank.loan.entity.LoanInstallment;
import com.springbank.loan.repository.LoanInstallmentRepository;
import com.springbank.notification.entity.Notification;
import com.springbank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstallmentReminderJob {

    private final LoanInstallmentRepository installmentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * ?") // Every day at 9:00 AM
    @Transactional
    public void remindOverdueInstallments() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Upcoming due tomorrow
        List<LoanInstallment> upcoming = installmentRepository.findByDueDateBetweenAndStatus(today, tomorrow, InstallmentStatus.PENDING);

        for (LoanInstallment inst : upcoming) {
            if (inst.isOverdue()) {
                notificationService.createNotification(Notification.builder()
                        .type(NotificationType.INSTALLMENT_OVERDUE)
                        .title("قسط دیرکرد دارد")
                        .message("قسط شماره " + inst.getInstallmentNumber() + " از وام شما با مبلغ " + inst.getAmount() + " دیرکرد دارد.")
                        .channel(NotificationChannel.IN_APP)
                        .user(inst.getLoan().getUser())
                        .build());
                log.info("Overdue reminder sent for installment {}", inst.getId());
            } else {
                notificationService.createNotification(Notification.builder()
                        .type(NotificationType.INSTALLMENT_DUE)
                        .title("یادآوری قسط")
                        .message("قسط شماره " + inst.getInstallmentNumber() + " با مبلغ " + inst.getAmount() + " فردا سررسید می‌شود.")
                        .channel(NotificationChannel.IN_APP)
                        .user(inst.getLoan().getUser())
                        .build());
                log.info("Upcoming reminder sent for installment {}", inst.getId());
            }
        }

        log.info("Installment reminder job executed. {} reminders sent.", upcoming.size());
    }
}
