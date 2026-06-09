package com.springbank.common.exception;

import com.springbank.common.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER — مدیریت متمرکز خطاها
 * ============================================================================
 * تمام exceptionها به فرمت استاندارد {@link ApiResponse} تبدیل می‌شوند.
 *
 * اصول امنیتی:
 *  - جزئیات فنی (URL داخلی، stack trace، نام سرویس‌ها) فقط در لاگ سمت سرور می‌رود.
 *  - به کلاینت پیام عمومی و امن برگردانده می‌شود (جلوگیری از نشت اطلاعات معماری).
 *
 * ترتیب هندلرها مهم است: زیرکلاس‌ها قبل از کلاس پدر می‌آیند
 * (ResourceNotFoundException قبل از BusinessException).
 * ============================================================================
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========================= 404 — Not Found =========================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("[ERR-404] Resource not found: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        log.warn("[ERR-404] Entity not found: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.NOT_FOUND, "منبع مورد نظر یافت نشد.", request);
    }

    // ========================= 400 — Bad Request =========================

    /**
     * خطای اعتبارسنجی {@code @Valid} روی بدنه‌ی درخواست — همه‌ی فیلدهای نامعتبر را برمی‌گرداند.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex,
                                                                             WebRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "مقدار نامعتبر" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        log.warn("[ERR-400] Validation failed: {} | Path: {}", fieldErrors, path(request));
        ApiResponse<Map<String, String>> body = new ApiResponse<>(
                false, "اعتبارسنجی ورودی ناموفق بود.", fieldErrors,
                HttpStatus.BAD_REQUEST.value(), path(request), java.time.LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                       WebRequest request) {
        log.warn("[ERR-400] Constraint violation: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.BAD_REQUEST, "پارامترهای درخواست نامعتبر هستند.", request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("[ERR-400] Business error: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("[ERR-400] Illegal argument: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("[ERR-400] Upload too large: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.BAD_REQUEST, "حجم فایل بارگذاری‌شده بیش از حد مجاز است.", request);
    }

    // ========================= 401 — Unauthorized =========================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.warn("[ERR-401] Bad credentials | Path: {}", path(request));
        // پیام عمومی تا مشخص نشود نام کاربری یا رمز کدام اشتباه است (user enumeration)
        return build(HttpStatus.UNAUTHORIZED, "نام کاربری یا رمز عبور اشتباه است.", request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex, WebRequest request) {
        log.warn("[ERR-423] Account locked | Path: {}", path(request));
        return build(HttpStatus.LOCKED, ex.getMessage(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex, WebRequest request) {
        log.warn("[ERR-401] Account disabled | Path: {}", path(request));
        return build(HttpStatus.UNAUTHORIZED, "حساب کاربری غیرفعال است.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex, WebRequest request) {
        log.warn("[ERR-401] Authentication failed: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.UNAUTHORIZED, "احراز هویت ناموفق بود.", request);
    }

    // ========================= 403 — Forbidden =========================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("[ERR-403] Access denied | Path: {}", path(request));
        return build(HttpStatus.FORBIDDEN, "شما مجوز دسترسی به این منبع را ندارید.", request);
    }

    // ========================= 409 — Conflict =========================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.warn("[ERR-409] Illegal state: {} | Path: {}", ex.getMessage(), path(request));
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("[ERR-409] Optimistic lock conflict | Path: {}", path(request));
        return build(HttpStatus.CONFLICT,
                "این رکورد همزمان توسط عملیات دیگری تغییر کرده است. لطفاً دوباره تلاش کنید.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        // جزئیات SQL/constraint نباید به کلاینت برود
        log.error("[ERR-409] Data integrity violation | Path: {} | Detail: {}", path(request), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "عملیات به دلیل تداخل داده‌ها انجام نشد (مثلاً مقدار تکراری).", request);
    }

    // ========================= 500 — Internal =========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, WebRequest request) {
        // فقط در لاگ سمت سرور جزئیات کامل ثبت می‌شود؛ به کلاینت پیام عمومی می‌رود.
        log.error("[ERR-500] Unexpected error at path {}: ", path(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "خطای داخلی سرور رخ داد. لطفاً بعداً دوباره تلاش کنید.", request);
    }

    // ========================= Helpers =========================

    private static String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private static ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, status.value(), path(request)));
    }
}
