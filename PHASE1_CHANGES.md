# 🚀 فاز ۱ — ثبت‌نام، احراز هویت اولیه، قفل حساب و KYC

این سند تغییرات فاز ۱ و مشکلاتی که در این فاز پیدا و رفع شد را توضیح می‌دهد.
**به فایل‌های `application.yml` و تنظیمات RabbitMQ دست زده نشده است.**

---

## ✅ فلوهای پیاده‌سازی‌شده

### فلوی ۱ — ثبت‌نام و احراز هویت اولیه
- ✅ ثبت‌نام کاربر با **اعتبارسنجی کامل ورودی** (نام کاربری، رمز قوی، ایمیل، موبایل).
- ✅ اختصاص خودکار نقش پیش‌فرض **`ROLE_CUSTOMER`** هنگام ثبت‌نام.
- ✅ صدور **توکن JWT** (Access + Refresh) در ورود. (طبق تصمیم شما stateless ماند؛ logout سمت کلاینت.)
- ✅ **شمارش تلاش‌های ناموفق ورود** و **قفل ۳۰ دقیقه‌ای پس از ۵ تلاش** — حالا واقعاً به فرآیند login وصل شد.
- ✅ پیام دقیق «N دقیقه دیگر تلاش کنید» هنگام قفل بودن.

### فلوی ۲ — احراز هویت (KYC)
- ✅ ساخت رکورد `KycVerification` و **بارگذاری واقعی مدارک** (کارت ملی + سلفی) روی دیسک سرور.
- ✅ پس از بارگذاری → وضعیت **`DOCUMENT_UPLOADED`**.
- ✅ بررسی توسط **`ROLE_MANAGER`** (و ADMIN): `start-review` → `UNDER_REVIEW`، سپس `APPROVED`/`REJECTED`.
- ✅ کنترل دسترسی درست: کاربر فقط KYC خودش را می‌بیند؛ لیست بررسی فقط برای MANAGER/ADMIN.

---

## 🔴 باگ‌های بحرانی که در فاز ۱ کشف و رفع شد

| # | مشکل | جزئیات | رفع |
|---|------|--------|-----|
| B1 | **ثبت‌نام همیشه می‌شکست** | `RoleRepository.findDefaultUserRole()` دنبال نقش `ROLE_USER` می‌گشت ولی `DataInitializer` فقط `ADMIN`/`CUSTOMER` می‌ساخت → خطای «Default role not found». | کوئری به نقش `CUSTOMER` اصلاح شد (= `ROLE_CUSTOMER` در Spring Security). |
| B2 | **قفل حساب اصلاً کار نمی‌کرد** | `AuthController` مستقیم `authenticate` می‌کرد و هیچ‌وقت `recordFailedLogin`/`recordSuccessfulLogin` صدا زده نمی‌شد. | `AuthService` + `LoginAttemptService` اضافه شد و کل منطق lockout به login وصل شد. |
| B3 | **نبود اعتبارسنجی ورودی** | هیچ `@Valid` و هیچ constraint روی DTOها نبود. | DTOهای ثبت‌نام/ورود/KYC با `jakarta.validation` و کنترلرها با `@Valid` مجهز شدند + هندلر `MethodArgumentNotValidException`. |
| B4 | **نقش MANAGER وجود نداشت** | فلوی KYC به `ROLE_MANAGER` نیاز داشت ولی ساخته نمی‌شد. | نقش `MANAGER` و کاربر نمونه `manager/manager123` به `DataInitializer` اضافه شد. |
| B5 | **self-invocation در تراکنش** | اگر منطق DB داخل همان bean صدا زده می‌شد، `@Transactional` بی‌اثر می‌ماند. | منطق شمارنده به bean جدای `LoginAttemptService` منتقل شد. |

---

## 🛠️ بهبودهای کیفی/امنیتی فاز ۱

- **`GlobalExceptionHandler` بازنویسی شد**: هندلر برای validation، `BadCredentials` (پیام عمومی برای جلوگیری از user-enumeration)، `Locked`, `AccessDenied`, `OptimisticLocking`, `DataIntegrity` و... ؛ و **حذف نشت اطلاعات** (دیگر URL داخلی/استک‌تریس به کلاینت نمی‌رود — فقط در لاگ سرور).
- **`DataInitializer` با `@Profile("!prod")`**: داده‌ی نمونه و کاربران تست فقط خارج از production ساخته می‌شوند (بدون دست‌زدن به yaml).
- **`FileStorageService`** امن: فقط JPG/PNG/PDF، سقف ۵MB، نام فایل تصادفی (UUID)، محافظت در برابر path traversal.

---

## 📡 Endpointهای فاز ۱

```
POST /api/auth/register                 ثبت‌نام (نقش پیش‌فرض ROLE_CUSTOMER)
POST /api/auth/login                    ورود (+ مدیریت قفل حساب)
POST /api/auth/refresh                  تازه‌سازی توکن

POST /api/kyc/{userId}/documents        بارگذاری مدارک (multipart: nationalId, selfie, [addressProof]) → DOCUMENT_UPLOADED
POST /api/kyc/submit                    ثبت KYC با مسیر مدارک آماده (JSON)
GET  /api/kyc/user/{userId}             مشاهده KYC (خود کاربر یا MANAGER/ADMIN)
GET  /api/kyc?status=DOCUMENT_UPLOADED  لیست برای پنل مدیر (MANAGER/ADMIN)
POST /api/kyc/{id}/start-review         شروع بررسی → UNDER_REVIEW (MANAGER/ADMIN)
POST /api/kyc/{id}/review               تأیید/رد نهایی (MANAGER/ADMIN)
```

### نمونه‌ی تست سریع
```bash
# 1) ثبت‌نام
curl -X POST localhost:8081/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"ali123","password":"Passw0rd!","email":"ali@test.com","phoneNumber":"09120000001"}'

# 2) ورود
curl -X POST localhost:8081/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"ali123","password":"Passw0rd!"}'

# 3) بارگذاری مدارک KYC (با توکن کاربر)
curl -X POST localhost:8081/api/kyc/<userId>/documents -H 'Authorization: Bearer <TOKEN>' \
  -F nationalId=@id.jpg -F selfie=@selfie.jpg

# 4) مدیر تأیید می‌کند (با توکن manager)
curl -X POST localhost:8081/api/kyc/<kycId>/review -H 'Authorization: Bearer <MANAGER_TOKEN>' \
  -H 'Content-Type: application/json' -d '{"status":"APPROVED","approvedLevel":"STANDARD"}'
```

---

## ⚙️ پیکربندی قابل‌تنظیم (با مقدار پیش‌فرض درون‌کد — بدون نیاز به تغییر yaml)
```
app.security.max-failed-attempts   = 5     (پیش‌فرض)
app.security.lock-duration-minutes = 30    (پیش‌فرض)
app.storage.kyc-dir                = ./uploads/kyc  (پیش‌فرض)
```
اگر بعداً خواستید این‌ها را در yaml ست کنید، فقط همین کلیدها را اضافه کنید (اختیاری است).

---

## 📋 فایل‌های تغییریافته/جدید در فاز ۱
```
新 user/service/AuthService.java
新 user/service/LoginAttemptService.java
新 common/storage/FileStorageService.java
✎ user/controller/AuthController.java         (بازنویسی: @Valid، IP، AuthService)
✎ user/controller/KycController.java          (آپلود فایل، start-review، دسترسی MANAGER)
✎ user/service/KycWriteService.java           (آپلود واقعی، چرخه‌ی وضعیت)
✎ common/exception/GlobalExceptionHandler.java (بازنویسی کامل)
✎ config/DataInitializer.java                 (نقش/کاربر MANAGER، @Profile("!prod"))
✎ user/repository/RoleRepository.java         (اصلاح نقش پیش‌فرض)
✎ user/security/JwtTokenProvider.java         (متد getAccessTokenExpirationMs)
✎ user/dto/UserRegistrationDto.java           (validation)
✎ user/dto/LoginRequestDto.java               (validation)
✎ user/dto/RefreshTokenRequestDto.java        (validation)
✎ user/dto/KycSubmitDto.java                  (validation)
✎ user/dto/KycReviewDto.java                  (validation)
```

> فاز بعد (فاز ۲): فلوهای ۳ تا ۵ — افتتاح حساب + صدور خودکار کارت مجازی، شارژ حساب، و **انتقال وجه اتمیک** با بررسی سقف روزانه/ماهانه و نوتیفیکیشن.
