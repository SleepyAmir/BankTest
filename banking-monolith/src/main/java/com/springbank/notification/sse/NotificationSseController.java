package com.springbank.notification.sse;

import com.springbank.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================================
 * NOTIFICATION SSE CONTROLLER — استریم زنده‌ی نوتیفیکیشن به ازای هر کاربر (فلوی ۱۱)
 * ============================================================================
 * هر کاربر به یک کانال اختصاصی متصل می‌شود و فقط نوتیفیکیشن‌های خودش را دریافت می‌کند.
 * (بهبود نسبت به نسخه‌ی قبلی که همه‌ی پیام‌ها را برای همه پخش می‌کرد.)
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationSseController {

    /** نگاشت userId → فهرست emitterهای فعال آن کاربر (پشتیبانی از چند تب/دستگاه). */
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        Long userId = currentUserId(authentication);
        SseEmitter emitter = new SseEmitter(0L); // بدون timeout

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        log.debug("[SSE] کاربر {} به استریم نوتیفیکیشن متصل شد", userId);
        return emitter;
    }

    /** ارسال یک رویداد فقط به کاربر مشخص. */
    public void sendToUser(Long userId, String payload) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] کاربر {} اتصال فعالی ندارد؛ نوتیفیکیشن فقط در DB ذخیره شد", userId);
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser su) {
            return su.getId();
        }
        throw new IllegalStateException("کاربر احراز هویت نشده است");
    }
}
