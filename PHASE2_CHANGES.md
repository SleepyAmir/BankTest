# 🚀 فاز ۲ — افتتاح حساب + کارت، شارژ، و انتقال وجه اتمیک

تغییرات فاز ۲ (فلوهای ۳، ۴، ۵). **به فایل‌های `application.yml` و تنظیمات RabbitMQ دست زده نشده است.**

---

## ✅ فلوهای پیاده‌سازی‌شده

### فلوی ۳ — افتتاح حساب و دریافت کارت
- ✅ `POST /api/accounts/open` — کاربر با **نوع حساب** (CHECKING/SAVINGS) و **کد شعبه** درخواست می‌دهد.
- ✅ پیش‌نیاز بانکی: **KYC باید APPROVED باشد** وگرنه افتتاح حساب رد می‌شود.
- ✅ حساب با **شماره‌ی یکتا**، **موجودی صفر** و وضعیت **ACTIVE** ساخته می‌شود؛ شعبه با `branch.code` انتخاب می‌شود.
- ✅ **بلافاصله کارت مجازی فعال** صادر می‌شود: شماره ۱۶ رقمی معتبر (Luhn)، CVV2، PIN، انقضای ۴ ساله، سقف روزانه/ماهانه.
- ✅ CVV2/PIN فقط **یک‌بار** هنگام صدور به‌صورت آشکار برگردانده می‌شوند و در DB **هش** ذخیره می‌شوند.

### فلوی ۴ — شارژ حساب
- ✅ `POST /api/transactions/deposit` — مبلغ به حساب اضافه، وضعیت **COMPLETED**، **کد پیگیری یکتا**، دسته‌بندی پیش‌فرض `salary`.
- ✅ انتشار `TransactionCompletedEvent` به RabbitMQ + نوتیفیکیشن IN_APP.

### فلوی ۵ — انتقال وجه داخلی (اتمیک)
- ✅ `POST /api/transactions/transfer` — انتقال بین دو حساب در **یک تراکنش اتمیک**.
- ✅ بررسی‌ها: حساب مبدأ **isActive()**، **موجودی کافی**، **سقف روزانه/ماهانه**.
- ✅ سقف مؤثر = **min(سقف حساب، سقف KYC)** — امن‌ترین حالت.
- ✅ از مبدأ کم و به مقصد اضافه می‌شود؛ مصرف روزانه/ماهانه ثبت و در شروع روز/ماه ریست می‌شود.
- ✅ **نوتیفیکیشن IN_APP برای هر دو طرف** (مبدأ و مقصد) + ارسال زنده‌ی SSE.
- ✅ انتشار `TransactionCompletedEvent` تا میکروسرویس‌ها (read/fraud/analytics/audit) رکورد بسازند.

---

## 🔴 باگ‌های بحرانی که در فاز ۲ کشف و رفع شد

| # | مشکل | جزئیات | رفع |
|---|------|--------|-----|
| B6 | **انتقال پول اتمیک نبود** | نسخه‌ی قبلی در `transaction-write` با **دو فراخوانی REST جدا** (withdraw سپس deposit) پول جابجا می‌کرد؛ اگر دومی fail می‌شد پول گم می‌شد. | `MoneyMovementService` پول را در **یک @Transactional داخل monolith** جابجا می‌کند (rollback کامل در صورت خطا). |
| B7 | **مبلغ منفی/صفر پذیرفته می‌شد** | `Account.deposit/withdraw` مبلغ منفی را چک نمی‌کرد → `withdraw(-1000)` موجودی را زیاد می‌کرد! | اعتبارسنجی `requirePositive` در entity + `@DecimalMin` در DTOها. |
| B8 | **باگ امنیتی در `@PreAuthorize`** | در `AccountController` از `#accountReadService` (متغیر ناموجود) به‌جای `@accountReadService` (bean) استفاده شده بود → SpEL خطا/رد دسترسی. | همه به `@accountReadService` اصلاح شد. |
| B9 | **شعبه‌ای وجود نداشت** | فلوی افتتاح حساب به Branch نیاز دارد ولی `DataInitializer` هیچ شعبه‌ای نمی‌ساخت. | شعبه‌ی نمونه `BR001` اضافه شد. |
| B10 | **race condition انتقال همزمان** | دابل‌اسپندینگ با درخواست‌های همزمان. | با `@Version` (optimistic locking در BaseEntity) + هندلر `OptimisticLockingFailureException` پوشش داده شد. |
| B11 | **SSE همه‌ی پیام‌ها را برای همه پخش می‌کرد** | `NotificationSseController` تک‌کاناله بود → نشت نوتیفیکیشن بین کاربران. | به **per-user** ارتقا یافت (هر کاربر فقط نوتیفیکیشن خودش). |

---

## 📡 Endpointهای فاز ۲
```
POST /api/accounts/open          افتتاح حساب + صدور خودکار کارت (نیازمند KYC تأییدشده)
POST /api/transactions/deposit   شارژ حساب (DEPOSIT)
POST /api/transactions/transfer  انتقال وجه داخلی اتمیک (TRANSFER)
GET  /api/notifications/stream    استریم زنده‌ی نوتیفیکیشن (SSE، per-user)
```

### نمونه‌ی تست
```bash
# افتتاح حساب (کاربر باید KYC=APPROVED داشته باشد؛ کاربر admin در seed تأیید شده است)
curl -X POST localhost:8081/api/accounts/open -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"type":"CHECKING","branchCode":"BR001","alias":"حساب اصلی"}'

# شارژ
curl -X POST localhost:8081/api/transactions/deposit -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"accountId":1,"amount":5000000,"spendingCategory":"salary"}'

# انتقال
curl -X POST localhost:8081/api/transactions/transfer -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":1000000}'
```

---

## 📋 فایل‌های جدید/تغییریافته در فاز ۲

### 🆕 جدید
```
account/dto/OpenAccountDto.java
account/dto/AccountWithCardDto.java
account/dto/DepositRequestDto.java
account/dto/TransferRequestDto.java
account/dto/TransactionResultDto.java
account/service/MoneyMovementService.java
account/controller/MoneyMovementController.java
card/dto/IssuedCardDto.java
common/util/CardNumberGenerator.java
common/util/AccountNumberGenerator.java
```

### ✎ تغییریافته
```
account/entity/Account.java                  (مبلغ مثبت، شمارنده‌های مصرف و reset)
account/service/AccountWriteService.java      (متد openAccount + صدور کارت)
account/controller/AccountController.java     (endpoint open، @Valid، اصلاح @PreAuthorize)
account/dto/AccountCreateDto.java             (validation)
card/service/CardWriteService.java            (issueVirtualCardForAccount)
notification/service/NotificationService.java (notifyInApp + SSE)
notification/sse/NotificationSseController.java (per-user)
config/DataInitializer.java                   (شعبه‌ی BR001)
```

> فاز بعد (فاز ۳): فلوهای ۶ (تشخیص تقلب)، ۷ (AML)، و ۱۱ (تکمیل اعلان‌ها) — در میکروسرویس‌های fraud/analytics با مصرف همین `TransactionCompletedEvent`.
