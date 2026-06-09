package com.springbank.user.service;

import com.springbank.user.dto.LoginRequestDto;
import com.springbank.user.dto.TokenResponseDto;
import com.springbank.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * AUTH SERVICE — منطق ورود، قفل حساب و صدور توکن (فلوی ۱)
 * ============================================================================
 *  - بررسی قفل حساب «قبل» از احراز هویت.
 *  - شمارش تلاش‌های ناموفق و قفل ۳۰ دقیقه‌ای پس از ۵ تلاش (در LoginAttemptService).
 *  - صفر کردن شمارنده و ثبت IP در ورود موفق.
 *  - صدور Access/Refresh Token (JWT stateless؛ logout سمت کلاینت).
 *
 * عملیات DBی مربوط به شمارنده در {@link LoginAttemptService} (bean جدا) انجام می‌شود
 * تا تراکنش از طریق پروکسی Spring اعمال شود.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.expiration:3600000}")
    private long jwtExpirationMs;

    public TokenResponseDto login(LoginRequestDto request, String ipAddress) {
        String username = request.username();
        log.info("[AUTH-LOGIN] تلاش برای ورود کاربر: {}", username);

        // گام ۱: اگر حساب قفل است، قبل از هر کاری رد کن
        loginAttemptService.ensureNotLocked(username);

        try {
            // گام ۲: احراز هویت
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );

            // گام ۳: ورود موفق → صفر کردن شمارنده و ثبت IP
            loginAttemptService.recordSuccess(username, ipAddress);

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            int expiresInSeconds = (int) (jwtExpirationMs / 1000);

            log.info("[AUTH-LOGIN] ✅ ورود موفق برای کاربر: {}", username);
            return new TokenResponseDto(accessToken, refreshToken, "Bearer", expiresInSeconds);

        } catch (BadCredentialsException ex) {
            // گام ۴: ورود ناموفق → افزایش شمارنده و در صورت لزوم قفل کردن
            loginAttemptService.recordFailure(username);
            throw ex; // GlobalExceptionHandler به پیام امن تبدیل می‌کند
        }
    }
}
