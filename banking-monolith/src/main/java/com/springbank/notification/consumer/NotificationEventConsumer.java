package com.springbank.notification.consumer;

import com.springbank.common.event.FraudDetectedEvent;
import com.springbank.common.event.LoanApprovedEvent;
import com.springbank.common.event.TransactionBlockedEvent;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.common.enums.NotificationChannel;
import com.springbank.common.enums.NotificationType;
import com.springbank.notification.entity.Notification;
import com.springbank.notification.service.NotificationService;
import com.springbank.notification.sse.NotificationSseController;
import com.springbank.user.entity.User;
import com.springbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * NOTIFICATION EVENT CONSUMER
 * ============================================================================
 * Listens to: notification.queue (bound to routing keys: notification.*, transaction.*, loan.*)
 * Actions: Creates Notification record + Broadcasts via SSE (real-time push)
 *
 * LOG MARKERS: [NOTIF-RECV] [NOTIF-SAVE] [NOTIF-SSE] [NOTIF-ERR]
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "notification.queue")
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final UserService userService;
    private final NotificationSseController sseController;

    @RabbitHandler
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[NOTIF-RECV] Received TransactionCompletedEvent: txId={}, userId={}, amount={}",
                event.getTransactionId(), event.getUserId(), event.getAmount());
        if (event.getUserId() != null) {
            var notification = createNotification(event.getUserId(), NotificationType.TRANSACTION_DONE,
                    "Transaction Completed",
                    "Your transaction of " + event.getAmount() + " " + event.getType() + " has been completed.");
            if (notification != null) {
                pushSse(event.getUserId(), "TRANSACTION_DONE", "Transaction Completed", notification.getMessage());
            }
        } else {
            log.warn("[NOTIF-ERR] TransactionCompletedEvent has null userId — cannot send notification!");
        }
    }

    @RabbitHandler
    public void handleLoanApproved(LoanApprovedEvent event) {
        log.info("[NOTIF-RECV] Received LoanApprovedEvent: loanId={}, userId={}, amount={}",
                event.getLoanId(), event.getUserId(), event.getAmount());
        var notification = createNotification(event.getUserId(), NotificationType.LOAN_APPROVED,
                "Loan Approved",
                "Your loan application for " + event.getAmount() + " has been approved.");
        if (notification != null) {
            pushSse(event.getUserId(), "LOAN_APPROVED", "Loan Approved", notification.getMessage());
        }
    }

    @RabbitHandler
    public void handleFraudDetected(FraudDetectedEvent event) {
        log.info("[NOTIF-RECV] Received FraudDetectedEvent: userId={}, riskLevel={}",
                event.getUserId(), event.getRiskLevel());
        var notification = createNotification(event.getUserId(), NotificationType.FRAUD_ALERT,
                "Fraud Alert",
                "A suspicious transaction has been detected on your account. Risk level: " + event.getRiskLevel());
        if (notification != null) {
            pushSse(event.getUserId(), "FRAUD_ALERT", "Fraud Alert", notification.getMessage());
        }
    }

    @RabbitHandler
    public void handleTransactionBlocked(TransactionBlockedEvent event) {
        log.info("[NOTIF-RECV] Received TransactionBlockedEvent: trackingCode={}, userId={}, riskLevel={}",
                event.getTrackingCode(), event.getUserId(), event.getRiskLevel());
        var notification = createNotification(event.getUserId(), NotificationType.FRAUD_ALERT,
                "تراکنش مسدود شد",
                "تراکنش شما به مبلغ " + event.getAmount() + " به دلیل ریسک تقلب مسدود شد. کد پیگیری: "
                        + event.getTrackingCode());
        if (notification != null) {
            pushSse(event.getUserId(), "TRANSACTION_BLOCKED", "تراکنش مسدود شد", notification.getMessage());
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object eventObject) {
        log.warn("[NOTIF-RECV] ⚠️ Unknown notification event type received: {}. Queue may have incompatible messages.",
                eventObject.getClass().getName());
    }

    private Notification createNotification(Long userId, NotificationType type, String title, String message) {
        log.info("[NOTIF-SAVE] Creating notification: userId={}, type={}, title={}", userId, type, title);
        try {
            User user = userService.getUserEntityById(userId);
            if (user == null) {
                log.error("[NOTIF-ERR] ❌ User not found for notification: userId={}. Notification will NOT be sent.", userId);
                return null;
            }
            Notification notification = Notification.builder()
                    .type(type)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .channel(NotificationChannel.IN_APP)
                    .user(user)
                    .build();
            Notification saved = notificationService.createNotification(notification);
            log.info("[NOTIF-SAVE] ✅ Notification saved: id={}, userId={}, type={}", saved.getId(), userId, type);
            return saved;
        } catch (Exception e) {
            log.error("[NOTIF-ERR] ❌ Failed to create notification for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * ارسال نوتیفیکیشن زنده فقط به کاربر مالک رویداد (per-user) از طریق SSE.
     * <p>
     * (پیش‌تر این متد به همه‌ی کاربران broadcast می‌کرد که باعث نشت نوتیفیکیشن می‌شد؛
     * اکنون با {@code sendToUser} فقط برای صاحب رویداد ارسال می‌شود.)
     */
    private void pushSse(Long userId, String eventType, String title, String message) {
        if (userId == null) {
            return;
        }
        try {
            String payload = String.format("{\"type\":\"%s\",\"title\":\"%s\",\"message\":\"%s\"}", eventType, title, message);
            sseController.sendToUser(userId, payload);
            log.info("[NOTIF-SSE] ✅ SSE sent to userId={}: type={}", userId, eventType);
        } catch (Exception e) {
            log.error("[NOTIF-SSE] ❌ SSE push failed for userId={}: {}", userId, e.getMessage());
        }
    }
}
