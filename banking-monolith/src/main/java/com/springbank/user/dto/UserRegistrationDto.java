package com.springbank.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * درخواست ثبت‌نام کاربر جدید.
 * <p>
 * تمام فیلدها با {@code jakarta.validation} اعتبارسنجی می‌شوند تا از ورود داده‌ی نامعتبر
 * به لایه‌ی سرویس جلوگیری شود. هنگام استفاده، کنترلر باید {@code @Valid} داشته باشد.
 */
public record UserRegistrationDto(

        @NotBlank(message = "نام کاربری الزامی است")
        @Size(min = 3, max = 50, message = "نام کاربری باید بین ۳ تا ۵۰ کاراکتر باشد")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "نام کاربری فقط می‌تواند شامل حروف انگلیسی، عدد و . _ - باشد")
        String username,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(min = 8, max = 100, message = "رمز عبور باید حداقل ۸ کاراکتر باشد")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "رمز عبور باید شامل حداقل یک حرف بزرگ، یک حرف کوچک و یک عدد باشد"
        )
        String password,

        @NotBlank(message = "ایمیل الزامی است")
        @Email(message = "فرمت ایمیل نامعتبر است")
        @Size(max = 100, message = "ایمیل نمی‌تواند بیش از ۱۰۰ کاراکتر باشد")
        String email,

        @Size(max = 50, message = "نام نمی‌تواند بیش از ۵۰ کاراکتر باشد")
        String firstName,

        @Size(max = 50, message = "نام خانوادگی نمی‌تواند بیش از ۵۰ کاراکتر باشد")
        String lastName,

        @Pattern(regexp = "^(09\\d{9})?$",
                message = "شماره موبایل باید با ۰۹ شروع شود و ۱۱ رقم باشد")
        String phoneNumber,

        String profilePictureUrl
) {}
