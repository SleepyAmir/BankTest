package com.springbank.user.security;

import com.springbank.user.repository.UserRepository;
import com.springbank.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * سرویس سفارشی بارگذاری اطلاعات کاربر برای Spring Security
 * <p>
 * این کلاس مسئولیت بارگذاری اطلاعات کاربر از دیتابیس و تبدیل آن به
 * شیء UserDetails مورد نیاز Spring Security را بر عهده دارد.
 * <p>
 * طبق اصول جداسازی مسئولیت‌ها (Separation of Concerns):
 * - این کلاس فقط به لایه امنیت تعلق دارد
 * - هیچ منطق تجاری در این کلاس پیاده‌سازی نمی‌شود
 * - تبدیل Entity به SecurityUser در این لایه انجام می‌شود
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * بارگذاری اطلاعات کاربر بر اساس نام کاربری
     *
     * @param username نام کاربری (می‌تواند null یا خالی باشد)
     * @return اطلاعات کاربر برای Spring Security
     * @throws UsernameNotFoundException اگر کاربر یافت نشود
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        log.debug("🔍 Loading user by username: {}", username);

        // اعتبارسنجی ورودی
        validateUsername(username);

        // بارگذاری کاربر با roles و permissions به صورت یکباره (JOIN FETCH)
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .map(SecurityUser::new)
                .orElseThrow(() -> {
                    log.warn("❌ User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

    /**
     * اعتبارسنجی نام کاربری
     *
     * @param username نام کاربری برای اعتبارسنجی
     * @throws UsernameNotFoundException اگر نام کاربری نامعتبر باشد
     */
    private void validateUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            log.warn("❌ Username is null or empty");
            throw new UsernameNotFoundException("Username cannot be empty");
        }
    }
}