package com.springbank.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * ============================================================================
 * FILE STORAGE SERVICE — ذخیره‌سازی فایل روی دیسک سرور (Local Storage)
 * ============================================================================
 * برای بارگذاری مدارک KYC (تصویر کارت ملی، سلفی، مدرک آدرس) استفاده می‌شود.
 *
 * امنیت:
 *  - فقط فرمت‌های مجاز تصویر/PDF پذیرفته می‌شوند.
 *  - نام فایل تصادفی (UUID) تولید می‌شود تا از path traversal و بازنویسی جلوگیری شود.
 *  - مسیر نهایی نسبت به دایرکتوری ریشه نرمال و بررسی می‌شود.
 *
 * مسیر ریشه از پراپرتی {@code app.storage.kyc-dir} خوانده می‌شود
 * (پیش‌فرض: ./uploads/kyc). به فایل‌های yaml دست زده نشده؛ مقدار پیش‌فرض درون‌کد است.
 * ============================================================================
 */
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private final Path rootDir;

    public FileStorageService(@Value("${app.storage.kyc-dir:./uploads/kyc}") String kycDir) {
        this.rootDir = Paths.get(kycDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
            log.info("[STORAGE] دایرکتوری ذخیره‌سازی KYC: {}", rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("امکان ساخت دایرکتوری ذخیره‌سازی فایل وجود ندارد: " + rootDir, e);
        }
    }

    /**
     * یک فایل را زیر یک زیرپوشه (مثلاً شناسه‌ی کاربر) ذخیره می‌کند و مسیر نسبی نهایی را برمی‌گرداند.
     *
     * @param file       فایل آپلودی
     * @param subFolder  زیرپوشه (مثلاً "user-12") — برای سازماندهی
     * @param logicalName نام منطقی فایل (مثلاً "national-id") — فقط برای خوانایی نام فایل
     * @return مسیر نسبی فایل ذخیره‌شده (برای ذخیره در DB)
     */
    public String store(MultipartFile file, String subFolder, String logicalName) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String safeSub = sanitize(subFolder);
        String filename = logicalName + "-" + UUID.randomUUID() + "." + extension;

        Path targetFolder = rootDir.resolve(safeSub).normalize();
        // محافظت در برابر path traversal
        if (!targetFolder.startsWith(rootDir)) {
            throw new IllegalArgumentException("مسیر ذخیره‌سازی نامعتبر است");
        }

        try {
            Files.createDirectories(targetFolder);
            Path target = targetFolder.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = rootDir.relativize(target).toString().replace('\\', '/');
            log.info("[STORAGE] ✅ فایل ذخیره شد: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("[STORAGE] ❌ خطا در ذخیره‌ی فایل: {}", e.getMessage());
            throw new IllegalStateException("ذخیره‌ی فایل ناموفق بود", e);
        }
    }

    // ===================== Validation Helpers =====================

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("فایل خالی است یا ارسال نشده");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("حجم فایل نباید بیش از ۵ مگابایت باشد");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("نوع فایل مجاز نیست. فقط JPG، PNG و PDF پذیرفته می‌شود");
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("پسوند فایل مجاز نیست: " + ext);
        }
    }

    private String extractExtension(String originalFilename) {
        String name = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IllegalArgumentException("فایل باید پسوند معتبر داشته باشد");
        }
        return name.substring(dot + 1).toLowerCase();
    }

    private String sanitize(String input) {
        if (input == null) return "misc";
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
