# 🚀 فاز ۳ — تشخیص تقلب (Fraud) و ضدپولشویی (AML)

تغییرات فاز ۳ (فلوهای ۶ و ۷) در میکروسرویس **fraud-service**.
**به فایل‌های `application.yml` و تنظیمات RabbitMQ دست زده نشده است.**

---

## ✅ فلوهای پیاده‌سازی‌شده

### فلوی ۶ — تشخیص تقلب
همزمان با هر `TransactionCompletedEvent`، یک **FraudAlert** ساخته و قوانین زیر اجرا می‌شوند:

| قانون | شرط | امتیاز ریسک |
|-------|------|-------------|
| **VELOCITY_CHECK** | بیش از ۳ تراکنش در یک دقیقه (از تاریخچه‌ی محلی) | +۴۰ |
| **UNUSUAL_TIME** | تراکنش در ساعات مشکوک (۰۰:۰۰–۰۵:۰۰) | +۲۰ |
| **AMOUNT_ANOMALY** | مبلغ بیش از ۵× میانگین ماهانه (از تاریخچه) | +۳۵ |
| **UNKNOWN_DEVICE** | نبود اثرانگشت دستگاه | +۱۵ |

- **سطح ریسک (RiskLevel):** `>=۸۰ BLOCK` · `>=۶۰ CHALLENGE` · `>=۳۰ REVIEW` · بقیه `ALLOW`.
- **Allow:** تراکنش بدون مشکل ادامه می‌یابد.
- **Block:** یک `TransactionBlockedEvent` منتشر می‌شود → تراکنش `BLOCKED` و نوتیفیکیشن به کاربر.

### فلوی ۷ — ضدپولشویی (AML)
همزمان با تکمیل تراکنش، چک‌های AML اجرا و در صورت برخورد **AmlAlert** ساخته می‌شود:

| نوع | شرط | درجه |
|-----|------|------|
| **LARGE_TRANSACTION** | مبلغ بیش از ۵۰۰ میلیون | MEDIUM |
| **ROUND_AMOUNT** | مبالغ گرد متوالی (مضرب ۱۰ میلیون، حداقل ۳ بار پشت‌سرهم) | MEDIUM |
| **STRUCTURING** | چند تراکنش کوچک‌تر با جمع نزدیک به سقف در ۲۴ ساعت | HIGH |

- همه‌ی هشدارها به **user و transaction** لینک می‌شوند و در پنل گزارش‌های مشکوک قابل مشاهده‌اند.

---

## 🏗️ تصمیم‌های معماری (طبق توافق)
1. **تاریخچه‌ی محلی:** fraud-service جدول سبک `fraud_transaction_history` را از همان eventها می‌سازد تا Velocity و Amount-Anomaly و Structuring را **مستقل** (بدون فراخوانی همزمان سرویس دیگر) محاسبه کند.
2. **بلاک async:** چون تراکنش در سرویس دیگری ثبت شده، fraud یک `TransactionBlockedEvent` منتشر می‌کند تا وضعیت BLOCKED شود (نه فراخوانی مستقیم).

---

## 🔴 باگ‌ها/نواقصی که در فاز ۳ رفع شد

| # | مشکل | رفع |
|---|------|-----|
| B12 | **قوانین تقلب واقعی وجود نداشت** | نسخه‌ی قبلی فقط Large-TX (>50M) و Unknown-device داشت؛ Velocity/Unusual-time/Amount-anomaly نبود. همه پیاده شد. |
| B13 | **AML فقط placeholder بود** | Round-Amount و Structuring اصلاً نبودند؛ آستانه‌ی Large نادرست بود. حالا طبق فلو (۵۰۰M) و با هر سه قانون. |
| B14 | **`transactionId` اجباری بود** | `FraudAlert.transactionId` با `nullable=false` بود؛ چون منبع جدید (monolith) شناسه‌ی عددی ندارد، nullable شد و `trackingCode` اضافه شد. |
| B15 | **حلقه‌ی پردازش رویداد** | `fraud.queue` با `transaction.#` bind است و رویداد بلاکِ خودِ fraud را دوباره می‌گرفت؛ consumer به الگوی `@RabbitHandler` تغییر کرد تا فقط `TransactionCompletedEvent` پردازش شود. |

---

## 📡 Endpointها (پنل کارمند — فلوی ۱۳)
```
GET  /api/fraud/alerts                  همه‌ی هشدارهای تقلب
GET  /api/fraud/alerts/blocked          تراکنش‌های مسدودشده (سطح BLOCK)
GET  /api/fraud/alerts/user/{userId}    هشدارهای تقلب یک کاربر
GET  /api/fraud/aml/user/{userId}       هشدارهای AML یک کاربر
POST /api/fraud/alerts/{id}/review      بررسی و حل هشدار تقلب
```

---

## ⚠️ نکته برای بهره‌برداری کامل
برای دقیق‌تر شدن قانون **UNKNOWN_DEVICE**، بهتر است `MoneyMovementService` (فاز ۲) هنگام انتشار event، فیلدهای `deviceFingerprint`/`ipAddress` را از درخواست کلاینت پر کند. در حال حاضر این فیلدها خالی‌اند و این قانون همیشه فعال می‌شود (می‌توان در فاز تکمیلی این فیلدها را به DTO انتقال/شارژ افزود).

---

## 📋 فایل‌های جدید/تغییریافته در فاز ۳

### 🆕 جدید
```
shared-kernel/.../common/event/TransactionBlockedEvent.java
fraud-service/.../fraud/entity/TransactionHistory.java
fraud-service/.../fraud/repository/TransactionHistoryRepository.java
fraud-service/.../fraud/service/AmlAnalysisService.java
fraud-service/.../fraud/messaging/FraudEventPublisher.java
```

### ✎ تغییریافته
```
fraud-service/.../fraud/service/FraudAnalysisService.java     (قوانین واقعی فلوی ۶ + فراخوانی AML + block)
fraud-service/.../fraud/consumer/FraudEventConsumer.java       (@RabbitHandler، جلوگیری از حلقه)
fraud-service/.../fraud/entity/FraudAlert.java                 (transactionId nullable + trackingCode)
fraud-service/.../fraud/controller/FraudAlertController.java   (endpoint /alerts/blocked)
banking-monolith/.../notification/consumer/NotificationEventConsumer.java  (هندلر TransactionBlockedEvent)
```

> فاز بعد (فاز ۴): فلوهای ۹ (وام و اعتبارسنجی)، ۱۰ (بازپرداخت اقساط)، ۱۲ (SpendingSnapshot)، و ۱۳ (تکمیل پنل کارمند/مدیر).
