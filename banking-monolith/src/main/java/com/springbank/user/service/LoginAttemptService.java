package com.springbank.user.service;

import com.springbank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * LOGIN ATTEMPT SERVICE — مدیریت تراکنشی شمارش تلاش‌ها و قفل حساب
 * ============================================================================
 * این منطق عمداً در یک bean جداگانه قرار دارد تا متدهای {@code @Transactional}
 * از طریق پروکسی Spring فراخوانی شوند (و دچار مشکل self-invocation در AuthService نشوند).
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    /** اگر حساب در حال حاضر قفل است، {@link LockedException} با زمان باقی‌مانده پرتاب می‌کند. */
    @Transactional(readOnly = true)
    public void ensureNotLocked(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            LocalDateTime lockedUntil = user.getLockedUntil();
            if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
                long minutesLeft = Math.max(1, Duration.between(LocalDateTime.now(), lockedUntil).toMinutes() + 1);
                log.warn("[AUTH-LOGIN] ⛔ حساب {} قفل است؛ {} دقیقه باقی مانده", username, minutesLeft);
                throw new LockedException(
                        "حساب کاربری به دلیل تلاش‌های ناموفق متعدد قفل شده است. "
                                + minutesLeft + " دقیقه دیگر دوباره تلاش کنید.");
            }
        });
    }

    @Transactional
    public void recordSuccess(String username, String ipAddress) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.recordSuccessfulLogin(ipAddress);
            userRepository.save(user);
            log.debug("[AUTH-LOGIN] شمارنده‌ی تلاش‌ها برای {} صفر شد", username);
        });
    }

    @Transactional
    public void recordFailure(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.recordFailedLogin(maxFailedAttempts, lockDurationMinutes);
            userRepository.save(user);

            int attempts = user.getFailedAttempts();
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                log.warn("[AUTH-LOGIN] 🔒 حساب {} پس از {} تلاش ناموفق برای {} دقیقه قفل شد",
                        username, attempts, lockDurationMinutes);
            } else {
                log.warn("[AUTH-LOGIN] ❌ ورود ناموفق برای {} ({}/{} تلاش)",
                        username, attempts, maxFailedAttempts);
            }
        });
    }
}
