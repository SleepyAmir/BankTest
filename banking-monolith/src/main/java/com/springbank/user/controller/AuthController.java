package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.user.dto.LoginRequestDto;
import com.springbank.user.dto.RefreshTokenRequestDto;
import com.springbank.user.dto.TokenResponseDto;
import com.springbank.user.dto.UserRegistrationDto;
import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.security.CustomUserDetailsService;
import com.springbank.user.security.JwtTokenProvider;
import com.springbank.user.service.AuthService;
import com.springbank.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * AUTH CONTROLLER — ثبت‌نام، ورود، تازه‌سازی توکن و خروج
 * ============================================================================
 * فلوی ۱:
 *  - ثبت‌نام → اختصاص نقش پیش‌فرض ROLE_CUSTOMER (در UserService).
 *  - ورود → بررسی قفل، شمارش تلاش‌های ناموفق، قفل ۳۰ دقیقه‌ای پس از ۵ تلاش (در AuthService).
 *  - توکن JWT صادر می‌شود؛ خروج (logout) توسط زنجیره‌ی امنیتی هندل می‌شود (سمت کلاینت توکن دور انداخته می‌شود).
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "ثبت‌نام، ورود، تازه‌سازی توکن")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Operation(summary = "ورود کاربر و دریافت توکن")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(@Valid @RequestBody LoginRequestDto request,
                                                               HttpServletRequest httpRequest) {
        TokenResponseDto tokens = authService.login(request, extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("ورود موفقیت‌آمیز بود", tokens, "/api/auth/login"));
    }

    @Operation(summary = "ثبت‌نام کاربر جدید (نقش پیش‌فرض: ROLE_CUSTOMER)")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody UserRegistrationDto dto) {
        UserResponseDto user = userService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ثبت‌نام با موفقیت انجام شد", user, "/api/auth/register"));
    }

    @Operation(summary = "تازه‌سازی توکن دسترسی با Refresh Token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh token نامعتبر یا منقضی شده است");
        }
        String username = jwtTokenProvider.getUsernameFromToken(request.refreshToken());
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateAccessToken(auth);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(auth);
        int expiresIn = (int) (jwtTokenProvider.getAccessTokenExpirationMs() / 1000);
        return ResponseEntity.ok(ApiResponse.success("توکن تازه‌سازی شد",
                new TokenResponseDto(newAccessToken, newRefreshToken, "Bearer", expiresIn),
                "/api/auth/refresh"));
    }

    /**
     * استخراج IP واقعی کلاینت با در نظر گرفتن هدر پروکسی.
     * توجه: در محیط production باید فقط به پروکسی‌های مورد اعتماد اتکا شود.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
