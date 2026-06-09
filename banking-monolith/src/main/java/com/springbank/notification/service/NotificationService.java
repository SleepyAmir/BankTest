package com.springbank.notification.service;

import com.springbank.common.enums.NotificationChannel;
import com.springbank.common.enums.NotificationType;
import com.springbank.notification.entity.Notification;
import com.springbank.notification.repository.NotificationRepository;
import com.springbank.notification.sse.NotificationSseController;
import com.springbank.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseController sseController;

    public List<Notification> getByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getAll() {
        return notificationRepository.findAllWithUser();
    }

    public Notification getById(Long id) {
        return notificationRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Notification n = getById(id);
        n.setIsRead(true);
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    /**
     * ساخت یک نوتیفیکیشن IN_APP، ذخیره در DB و ارسال زنده از طریق SSE (فلوهای ۵ و ۱۱).
     *
     * @param user          گیرنده
     * @param type          نوع نوتیفیکیشن
     * @param title         عنوان
     * @param message       متن پیام
     * @param transactionId شناسه‌ی تراکنش مرتبط (اختیاری)
     */
    @Transactional
    public Notification notifyInApp(User user, NotificationType type, String title, String message, Long transactionId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .transactionId(transactionId)
                .build();
        Notification saved = notificationRepository.save(notification);

        // ارسال زنده از طریق SSE (در صورت بروز خطا، نوتیفیکیشن ذخیره‌شده باقی می‌ماند)
        try {
            sseController.sendToUser(user.getId(),
                    String.format("{\"type\":\"%s\",\"title\":\"%s\",\"message\":\"%s\"}", type, title, message));
        } catch (Exception e) {
            log.warn("[NOTIF-SSE] ارسال زنده‌ی نوتیفیکیشن ناموفق بود: {}", e.getMessage());
        }
        return saved;
    }
}
