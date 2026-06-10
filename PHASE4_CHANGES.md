# 🚀 فاز ۴ — وام، بازپرداخت اقساط، اسنپ‌شات مالی و پنل مدیر

تغییرات فاز ۴ (فلوهای ۹، ۱۰، ۱۲، ۱۳).
**به فایل‌های `application.yml` و تنظیمات RabbitMQ دست زده نشده است.**

---

## ✅ فلوهای پیاده‌سازی‌شده

### فلوی ۹ — درخواست وام و اعتبارسنجی
- ✅ `POST /api/loans` — وام با وضعیت **PENDING** ثبت می‌شود (+ اعتبارسنجی ورودی).
- ✅ سیستم `creditScore` را بررسی و بر اساس آن **نرخ سود** (`getRecommendedInterestRate`) و **قسط ماهانه** (PMT) را تعیین می‌کند.
- ✅ **حداکثر مبلغ مجاز** بر اساس `getLoanMultiplier` چک می‌شود؛ درخواست بیش از سقف رد می‌شود.
- ✅ snapshot امتیاز اعتباری لحظه‌ی درخواست در وام ذخیره می‌شود.
- ✅ کارمند با `APPROVED` (ثبت `approvedBy`): وضعیت **ACTIVE**، `startDate/endDate`، `remainingAmount`.
- ✅ **مبلغ وام به‌صورت اتمیک به حساب کاربر واریز می‌شود** + تراکنش `LOAN_DISBURSEMENT`.
- ✅ تمام اقساط با سررسید ماهانه و تفکیک **اصل/سود** (amortization) تولید می‌شوند.

### فلوی ۱۰ — بازپرداخت اقساط
- ✅ `POST /api/loans/installments/{id}/pay` — تراکنش `LOAN_PAYMENT`.
- ✅ اگر با تأخیر باشد: **daysOverdue** محاسبه و **lateFee** (۲٪ ماهانه = نرخ روزانه × روز تأخیر) به قسط اضافه می‌شود.
- ✅ مبلغ قسط + جریمه **اتمیک از حساب کسر** می‌شود.
- ✅ `remainingAmount` کاهش می‌یابد؛ با پرداخت آخرین قسط، وام **COMPLETED** می‌شود.
- ✅ **امتیاز اعتباری** تحت تأثیر قرار می‌گیرد: پرداخت به‌موقع → افزایش امتیاز و فاکتور **PaymentHistory**؛ تأخیر → کاهش.

### فلوی ۱۲ — مدیریت مالی شخصی (SpendingSnapshot)
- ✅ اسنپ‌شات به‌صورت لحظه‌ای با هر تراکنش (از طریق رویداد) ساخته/به‌روزرسانی می‌شود: `totalIncome`، `totalExpense`، `transactionCount`، `savingsRate`، `topCategory`.
- ✅ **`comparedToPrevMonth`** (درصد تغییر هزینه نسبت به ماه قبل) محاسبه شد.
- ✅ `LOAN_DISBURSEMENT` به‌عنوان income در نظر گرفته می‌شود.
- ✅ **تسک پایان ماه** (`0 59 23 L * ?`) اسنپ‌شات‌های ماه را نهایی می‌کند.

### فلوی ۱۳ — پنل کارمند/مدیر
- ✅ نقش **MANAGER** به قابلیت‌های مدیریتی دسترسی یافت:
  - تأیید KYC (فاز ۱)، مدیریت وام (getAll/approve/reject)، هشدارهای AML و تراکنش‌های مسدودشده (فاز ۳).

---

## 🔴 باگ‌ها/نواقصی که در فاز ۴ رفع شد

| # | مشکل | رفع |
|---|------|-----|
| B16 | **سقف وام چک نمی‌شد** | کاربر می‌توانست هر مبلغی بخواهد؛ حالا با `getMaxAllowedLoanAmount` (مبتنی بر getLoanMultiplier) محدود شد. |
| B17 | **مبلغ وام واریز نمی‌شد** | هنگام تأیید، پول واقعاً به حساب اضافه نمی‌شد؛ حالا واریز اتمیک + تراکنش LOAN_DISBURSEMENT. |
| B18 | **پرداخت قسط، latefee را اعمال نمی‌کرد** | daysOverdue/lateFee محاسبه نمی‌شد و پول از حساب کم نمی‌شد؛ هر دو اصلاح شد. |
| B19 | **امتیاز اعتباری به‌روز نمی‌شد** | بعد از پرداخت، credit score و PaymentHistory تغییری نمی‌کرد؛ حالا اثر مثبت/منفی اعمال می‌شود. |
| B20 | **اقساط تفکیک اصل/سود نداشتند** | فقط مبلغ کل ست می‌شد؛ حالا principalPart/interestPart با جدول amortization. |
| B21 | **باگ SpEL در `@PreAuthorize`** | `#loanReadService` (متغیر ناموجود) → `@loanReadService` (bean) اصلاح شد. |
| B22 | **`comparedToPrevMonth` محاسبه نمی‌شد** | در AnalyticsService اضافه شد. |
| B23 | **`TransactionType` ناقص بود** | `LOAN_DISBURSEMENT` و `LOAN_PAYMENT` اضافه شد. |
| B24 | **متد `calculateLateFee` در entity باگ‌دار بود** | `new BigDecimal(days*double)` نادرست بود؛ با BigDecimal دقیق اصلاح شد. |

---

## 📡 Endpointهای کلیدی فاز ۴
```
POST /api/loans                          درخواست وام (PENDING)
POST /api/loans/{id}/approve             تأیید + واریز (ADMIN/MANAGER)
POST /api/loans/{id}/reject              رد وام (ADMIN/MANAGER)
GET  /api/loans/{id}/installments        مشاهده اقساط
POST /api/loans/installments/{id}/pay    پرداخت قسط (از حساب کسر می‌شود)
GET  /api/analytics/user/{userId}        اسنپ‌شات‌های مالی کاربر
```

---

## ⚙️ پیکربندی قابل‌تنظیم (پیش‌فرض درون‌کد — بدون نیاز به yaml)
```
app.loan.base-income-unit = 10000000   (واحد پایه برای محاسبه‌ی سقف وام = multiplier × این مقدار)
```

> **یادداشت طراحی:** چون «درآمد ماهانه‌ی واقعی» در مدل داده نیست، حداکثر وام = `getLoanMultiplier × واحد درآمد پایه` محاسبه می‌شود. اگر بعداً فیلد درآمد به User اضافه شد، می‌توان این فرمول را دقیق‌تر کرد.

---

## 📋 فایل‌های جدید/تغییریافته در فاز ۴

### 🆕 جدید
```
(هیچ فایل کاملاً جدیدی نیست — همه بهبود فایل‌های موجود)
```

### ✎ تغییریافته
```
shared-kernel/.../common/enums/TransactionType.java               (LOAN_DISBURSEMENT, LOAN_PAYMENT)
banking-monolith/.../loan/service/LoanWriteService.java            (سقف، واریز، latefee، credit score)
banking-monolith/.../loan/service/CreditScoreService.java          (سقف وام + بهبود امتیاز)
banking-monolith/.../loan/controller/LoanController.java           (@Valid، @loanReadService، MANAGER)
banking-monolith/.../loan/dto/LoanCreateDto.java                   (validation)
banking-monolith/.../loan/entity/LoanInstallment.java              (اصلاح calculateLateFee)
analytics-service/.../analytics/service/AnalyticsService.java      (comparedToPrevMonth، finalizeMonth)
analytics-service/.../analytics/scheduler/SpendingSnapshotScheduler.java  (تسک پایان ماه)
```

---

## 🎉 جمع‌بندی کل پروژه (فازهای ۱ تا ۴)
تمام ۱۳ فلوی درخواستی پیاده‌سازی شد:
۱ ثبت‌نام/lockout · ۲ KYC · ۳ افتتاح حساب+کارت · ۴ شارژ · ۵ انتقال اتمیک · ۶ تشخیص تقلب · ۷ AML · ۹ وام · ۱۰ اقساط · ۱۱ اعلان‌ها (SSE) · ۱۲ اسنپ‌شات مالی · ۱۳ پنل مدیر.

> در مجموع بیش از ۲۴ باگ/نقص بحرانی و امنیتی در طول فازها کشف و رفع شد.
