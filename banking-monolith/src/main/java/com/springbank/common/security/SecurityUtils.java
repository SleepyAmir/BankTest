package com.springbank.common.security;

import com.springbank.security.model.SecurityUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * ابزارهای کمکی امنیتی برای دسترسی به کاربر احرازشده‌ی فعلی.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    private static Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** شناسه‌ی کاربر فعلی (یا null اگر در دسترس نباشد). */
    public static Long currentUserId() {
        Authentication a = auth();
        if (a != null && a.getPrincipal() instanceof SecurityUser su) {
            return su.getId();
        }
        return null;
    }

    /** نام کاربری فعلی (یا null). */
    public static String currentUsername() {
        Authentication a = auth();
        return a != null ? a.getName() : null;
    }

    /** آیا کاربر فعلی یکی از نقش‌های داده‌شده را دارد؟ */
    public static boolean hasAnyRole(String... roles) {
        Authentication a = auth();
        if (a == null) return false;
        for (String role : roles) {
            String authority = "ROLE_" + role;
            if (a.getAuthorities().stream().anyMatch(x -> authority.equals(x.getAuthority()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * بررسی اینکه کاربر فعلی صاحب منبع است یا نقش staff (ADMIN/MANAGER) دارد.
     * در غیر این صورت {@link AccessDeniedException} پرتاب می‌کند.
     * <p>
     * staff همیشه مجاز است؛ در غیر این صورت id کاربر فعلی باید با owner برابر باشد.
     */
    public static void requireOwnerOrStaff(Long ownerUserId) {
        if (hasAnyRole("ADMIN", "MANAGER")) {
            return;
        }
        Long current = currentUserId();
        if (current != null && current.equals(ownerUserId)) {
            return;
        }
        throw new AccessDeniedException("شما مجوز دسترسی به این منبع را ندارید");
    }

    /**
     * شناسه‌ی کاربر هدف را تعیین می‌کند:
     *  - اگر کاربر فعلی staff (ADMIN/MANAGER) باشد، می‌تواند برای هر کاربری عمل کند (همان requestedUserId).
     *  - در غیر این صورت، همیشه id کاربر فعلی استفاده می‌شود (کاربر فقط برای خودش عمل می‌کند).
     * این روش از خطای 403 ناشی از ناهماهنگی id جلوگیری می‌کند و امن‌تر است.
     */
    public static Long resolveTargetUserId(Long requestedUserId) {
        if (hasAnyRole("ADMIN", "MANAGER")) {
            return requestedUserId != null ? requestedUserId : currentUserId();
        }
        Long current = currentUserId();
        if (current == null) {
            throw new AccessDeniedException("کاربر احراز هویت نشده است");
        }
        return current;
    }
}
