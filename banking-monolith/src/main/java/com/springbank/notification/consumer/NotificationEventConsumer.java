package com.springbank.notification.consumer;

import com.springbank.common.event.FraudDetectedEvent;
import com.springbank.common.event.LoanApprovedEvent;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.common.enums.NotificationChannel;
import com.springbank.common.enums.NotificationType;
import com.springbank.notification.entity.Notification;
import com.springbank.notification.service.NotificationService;
import com.springbank.notification.sse.NotificationSseController;
import com.springbank.user.entity.User;
import com.springbank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final NotificationSseController sseController;

    @RabbitListener(queues = "notification.queue")
    public void handleEvent(Object eventObject) {
        if (eventObject instanceof TransactionCompletedEvent event) {
            log.info("Notification: Transaction completed for user: {}", event.getUserId());
            if (event.getUserId() != null) {
                var notification = createNotification(event.getUserId(), NotificationType.TRANSACTION_DONE,
                        "Transaction Completed",
                        "Your transaction of " + event.getAmount() + " " + event.getType() + " has been completed.");
                if (notification != null) {
                    sseController.sendEvent("{" + "\"type\":\"TRANSACTION_DONE\",\"title\":\"Transaction Completed\",\"message\":\"" + notification.getMessage() + "\"}");
                }
            }
        } else if (eventObject instanceof LoanApprovedEvent event) {
            log.info("Notification: Loan approved for user: {}", event.getUserId());
            var notification = createNotification(event.getUserId(), NotificationType.LOAN_APPROVED,
                    "Loan Approved",
                    "Your loan application for " + event.getAmount() + " has been approved.");
            if (notification != null) {
                sseController.sendEvent("{" + "\"type\":\"LOAN_APPROVED\",\"title\":\"Loan Approved\",\"message\":\"" + notification.getMessage() + "\"}");
            }
        } else if (eventObject instanceof FraudDetectedEvent event) {
            log.info("Notification: Fraud detected for user: {}", event.getUserId());
            var notification = createNotification(event.getUserId(), NotificationType.FRAUD_ALERT,
                    "Fraud Alert",
                    "A suspicious transaction has been detected on your account. Risk level: " + event.getRiskLevel());
            if (notification != null) {
                sseController.sendEvent("{" + "\"type\":\"FRAUD_ALERT\",\"title\":\"Fraud Alert\",\"message\":\"" + notification.getMessage() + "\"}");
            }
        } else {
            log.warn("Unknown notification event type: {}", eventObject.getClass().getName());
        }
    }

    private Notification createNotification(Long userId, NotificationType type, String title, String message) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for notification: {}", userId);
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
        return notificationService.createNotification(notification);
    }
}
