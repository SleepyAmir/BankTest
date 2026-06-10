# 🔧 فاز ۵ — یکپارچه‌سازی تراکنش‌ها (transaction-write به‌عنوان تنها مرجع)

این فاز معماری تراکنش را تمیز و یکدست کرد تا `transaction-write` و `transaction-read` **به‌درستی و کامل** کار کنند و دیگر مسیر موازی وجود نداشته باشد.
**به فایل‌های `application.yml` و تنظیمات RabbitMQ دست زده نشده است.**

---

## 🎯 معماری نهایی (تمیز و اتمیک)

```
کلاینت / monolith(loan)
        │
        ▼
 transaction-write  ── یک فراخوانی اتمیک ──►  monolith /internal/accounts/transfer
 (تنها مرجع تراکنش)                            (withdraw + deposit در یک @Transactional)
        │ موفق شد
        ▼  status=COMPLETED، ذخیره، انتشار رویداد «transaction.completed»
        ▼
 transaction-read + fraud-service + analytics-service + notification(monolith)
```

- **اتمیک:** کل جابجایی پول در یک تراکنش DB سمت monolith → یا کامل انجام می‌شود یا rollback (پول گم نمی‌شود).
- **تنها یک منبع رویداد:** فقط `transaction-write` رویداد `transaction.completed` منتشر می‌کند → دیگر تراکنش تکراری در read model نیست.

---

## 🔴 مشکلاتی که در این فاز رفع شد

| # | مشکل | رفع |
|---|------|-----|
| B25 | **transaction-write غیر اتمیک بود** | دو فراخوانی REST جدا (withdraw سپس deposit)؛ اگر دومی fail می‌شد پول گم می‌شد. حالا یک فراخوانی اتمیک به `/internal/accounts/transfer`. |
| B26 | **دو مرحله‌ای بودن (create + complete)** | تراکنش PENDING می‌ماند تا دستی complete شود. حالا `createTransaction` یک‌مرحله‌ای و اتمیک است. |
| B27 | **دو مسیر موازی تراکنش** | هم monolith مستقیم (فاز ۲) هم transaction-write تراکنش می‌ساختند. مسیر مستقیم monolith حذف شد (`MoneyMovementController` و endpointهای `/api/accounts/{id}/deposit|withdraw`). |
| B28 | **وام مستقل تراکنش می‌ساخت** | حالا واریز وام و پرداخت قسط از طریق `transaction-write` انجام می‌شوند (یکدست). |
| B29 | **چک IP در `/internal/**` در Docker می‌شکست** | فقط `127.0.0.1` مجاز بود؛ در Docker آدرس داخلی (172.x) رد می‌شد و کل تراکنش fail می‌کرد. حالا رنج‌های شبکه‌ی خصوصی داخلی هم مجازند. |
| B30 | **نبود validation و timeout** | `TransactionCreateDto` با `@Valid`؛ RestTemplate در monolith با connect/read timeout. |

---

## 🔄 تغییر رفتار مهم (API)

**حذف شد** (مسیر موازی):
```
POST /api/accounts/{id}/deposit      ❌ حذف شد
POST /api/accounts/{id}/withdraw     ❌ حذف شد
POST /api/transactions/deposit       ❌ حذف شد (MoneyMovementController)
POST /api/transactions/transfer      ❌ حذف شد
POST /api/transactions/{id}/complete ❌ حذف شد (دیگر دو مرحله‌ای نیست)
```

**مرجع جدید برای همه‌ی تراکنش‌های پولی** (در سرویس transaction-write، پورت 8088):
```
POST /api/transactions
  body: { "type":"TRANSFER|DEPOSIT|WITHDRAWAL|...", "amount":..., "fromAccountId":..., "toAccountId":..., "userId":... }
POST /api/transactions/{id}/reverse
GET  /api/transactions/{id}
```

### نمونه
```bash
# انتقال (اتمیک، از طریق transaction-write)
curl -X POST localhost:8088/api/transactions -H 'Content-Type: application/json' \
  -d '{"type":"TRANSFER","amount":1000000,"fromAccountId":1,"toAccountId":2,"userId":1}'

# شارژ
curl -X POST localhost:8088/api/transactions -H 'Content-Type: application/json' \
  -d '{"type":"DEPOSIT","amount":5000000,"toAccountId":1,"userId":1,"spendingCategory":"salary"}'
```

---

## ⚙️ پیکربندی (پیش‌فرض درون‌کد — بدون نیاز به yaml)
```
services.transaction-write.url = http://localhost:8088   (در monolith، برای فراخوانی از LoanWriteService)
```
در Docker با متغیر محیطی `SERVICES_TRANSACTION-WRITE_URL` قابل override است (اختیاری).

---

## 📋 فایل‌های جدید/تغییریافته/حذف‌شده در این فاز

### 🆕 جدید
```
banking-monolith/.../account/dto/InternalTransferDto.java
banking-monolith/.../config/RestClientConfig.java
banking-monolith/.../loan/client/TransactionServiceClient.java
```

### ✎ تغییریافته
```
transaction-write/.../service/TransactionWriteService.java        (بازنویسی: اتمیک، یک‌مرحله‌ای)
transaction-write/.../controller/TransactionWriteController.java   (حذف complete، @Valid)
transaction-write/.../dto/request/TransactionCreateDto.java        (validation)
banking-monolith/.../account/service/MoneyMovementService.java     (بازنویسی: فقط اتمیک خام، بدون event)
banking-monolith/.../internal/InternalController.java              (endpoint transfer + اتمیک‌سازی deposit/withdraw)
banking-monolith/.../account/controller/AccountController.java     (حذف deposit/withdraw مستقیم)
banking-monolith/.../loan/service/LoanWriteService.java            (استفاده از transaction-write)
banking-monolith/.../config/SecurityConfig.java                   (اصلاح چک IP داخلی برای Docker)
```

### 🗑️ حذف‌شده
```
banking-monolith/.../account/controller/MoneyMovementController.java
banking-monolith/.../account/dto/DepositRequestDto.java
banking-monolith/.../account/dto/TransferRequestDto.java
```

---

## ✅ وضعیت transaction-read
**نیازی به تغییر نداشت** — از قبل با routing key `transaction.completed` گوش می‌داد (که ما هم همان را منتشر می‌کنیم)، dedup با trackingCode دارد، و همه‌ی فیلدها را از رویداد می‌سازد. حالا با تراکنش‌های واقعی پر می‌شود.
