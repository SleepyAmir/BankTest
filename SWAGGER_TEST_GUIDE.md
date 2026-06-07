# راهنمای تست Swagger — گام‌به‌گام با JSON نمونه

> ⚠️ **توجه**: DataInitializer قبلاً داده‌های اولیه را ساخته. می‌توانید فوراً با `admin` یا `customer` لاگین کنید.
>
> 🔑 **کاربرهای آماده (seed)**:
> - `admin` / `admin123` (role: ADMIN)
> - `customer` / `customer123` (role: CUSTOMER)
>
> 💳 **حساب و کارت آماده** (برای customer):
> - Account: `IR123456789012345678901234` (balance: 100,000,000)
> - Card: `6104331234567890`
> - Loan: PENDING (1,000,000,000)

---

## 🔗 آدرس Swagger هر سرویس

| سرویس | آدرس Swagger |
|-------|-------------|
| **Monolith (همه چیز)** | http://localhost:8081/swagger-ui.html |
| **Transaction-Write** | http://localhost:8088/swagger-ui.html |
| **Transaction-Read** | http://localhost:8087/swagger-ui.html |
| **Fraud** | http://localhost:8091/swagger-ui.html |
| **Analytics** | http://localhost:8093/swagger-ui.html |
| **Audit** | http://localhost:8092/swagger-ui.html |
| **Gateway** | http://localhost:8090 (فقط route — Swagger UI ندارد) |

---

## گام ۱: لاگین (Login) → دریافت JWT Token

**سرویس**: Monolith (8081)  
**Endpoint**: `POST /api/auth/login`

### JSON (admin):
```json
{
  "username": "admin",
  "password": "admin123"
}
```

### JSON (customer):
```json
{
  "username": "customer",
  "password": "customer123"
}
```

**پاسخ**: `token` و `refreshToken` برمی‌گرداند.  
**کپی کنید**: مقدار `token` را کپی کرده و در Swagger بالای صفحه → **Authorize** → `Bearer <token>` وارد کنید.

> 💡 **همه درخواست‌های بعدی** باید این Bearer Token را در Header داشته باشند.

---

## گام ۲: تست Monolith (8081) — User, Account, Card, Loan, Notification

### A) لیست کاربران (Admin Only)
**Endpoint**: `GET /api/users`  
**Token**: `admin` (چون customer role ندارد)

### B) لیست حساب‌ها
**Endpoint**: `GET /api/accounts`  
**Token**: هر کدام (admin یا customer)

### C) واریز به حساب (Deposit)
**Endpoint**: `POST /api/accounts/{id}/deposit`  
**Token**: admin یا customer  
**Path variable**: `id = 1` (حساب seed)  
**Query param**: `amount = 50000000`

> بدون Body — فقط `?amount=50000000` در URL

### D) ایجاد حساب جدید
**Endpoint**: `POST /api/accounts`  
```json
{
  "userId": 1,
  "accountNumber": "IR987654321098765432109876",
  "type": "SAVINGS",
  "alias": "Secondary Savings"
}
```

### E) لیست کارت‌ها
**Endpoint**: `GET /api/cards`  
**Token**: هر کدام

### F) ایجاد کارت جدید
**Endpoint**: `POST /api/cards`  
```json
{
  "accountId": 1,
  "cardNumber": "6104339999998888",
  "cvv2": "321",
  "pin": "5678",
  "expirationDate": "2029-06-30",
  "type": "CREDIT"
}
```

### G) لیست وام‌ها
**Endpoint**: `GET /api/loans`  
**Token**: admin (چون getAll فقط Admin هست)

### H) تایید وام (Approve Loan)
**Endpoint**: `POST /api/loans/{id}/approve`  
**Token**: admin  
**Path variable**: `id = 1` (وام seed)  
**Body**: ندارد (فقط Execute)

> بعد از approve، `LoanApprovedEvent` به RabbitMQ ارسال می‌شود و Notification می‌سازد.

### I) لیست نوتیفیکیشن‌ها
**Endpoint**: `GET /api/notifications`  
**Token**: admin

### J) نوتیفیکیشن‌های من
**Endpoint**: `GET /api/notifications/me`  
**Token**: customer (یا هر کاربر)

---

## گام ۳: تست Transaction-Write (8088) — ایجاد تراکنش

**مهم**: برای تست تراکنش، حتماً **Monolith** باید بالا باشد (balance check از طریق REST به Monolith می‌زند).

### A) ایجاد تراکنش (Transfer)
**Endpoint**: `POST /api/transactions`  
**Token**: از Monolith گرفته شده (همان token کار می‌کند)  

```json
{
  "amount": 25000000,
  "currency": "IRR",
  "type": "TRANSFER",
  "description": "Test transfer from seed account",
  "fromAccountId": 1,
  "toAccountId": 2,
  "cardId": null,
  "loanInstallmentId": null,
  "spendingCategory": "TRANSFER",
  "ipAddress": "192.168.1.100",
  "deviceFingerprint": "device-abc-123",
  "location": "Tehran",
  "userId": 1
}
```

> ⚠️ **نکته**: `toAccountId: 2` باید وجود داشته باشد یا null بگذارید. اگر فقط یک حساب seed دارید، `toAccountId` را همان `1` بگذارید یا ابتدا حساب دوم بسازید.

### B) کامل کردن تراکنش (Complete)
**Endpoint**: `POST /api/transactions/{id}/complete`  
**Path variable**: `id = 1` (یا ID که از create گرفتید)  
**Body**: ندارد

### C) لیست تراکنش‌ها (در این سرویس)
**Endpoint**: `GET /api/transactions`  
**Token**: admin یا customer

---

## گام ۴: تست Transaction-Read (8087) — مدل خواندن CQRS

**سرویس**: Transaction-Read (8087)  
**توضیح**: این سرویس از RabbitMQ event می‌گیرد و تراکنش‌ها را در دیتابیس read خودش ذخیره می‌کند.

### A) لیست تراکنش‌ها (Read Model)
**Endpoint**: `GET /api/transactions`  
**Token**: همان Bearer Token

> اگر `transaction-write` تراکنشی ساخته و event ارسال کرده، اینجا باید ببینیدش.

### B) جستجو با Tracking Code
**Endpoint**: `GET /api/transactions/tracking/{trackingCode}`  
**Example**: `TXN...` (از transaction-write کپی کنید)

---

## گام ۵: تست Fraud-Service (8091) — تقلب و AML

**سرویس**: Fraud-Service (8091)

### A) لیست Fraud Alertها
**Endpoint**: `GET /api/fraud`  
**Token**: admin

### B) لیست AML Alertها
**Endpoint**: `GET /api/fraud/aml`  
**Token**: admin

### C) Fraud Alerts یک کاربر
**Endpoint**: `GET /api/fraud/user/{userId}`  
**Path variable**: `userId = 1`

> DataInitializer قبلاً یک FraudAlert (risk=75) و یک AmlAlert (HIGH) ساخته. باید ببینیدشان.

---

## گام ۶: تست Analytics-Service (8093) — تحلیل‌ها

**سرویس**: Analytics-Service (8093)

### A) Snapshot یک کاربر
**Endpoint**: `GET /api/analytics/user/{userId}`  
**Path variable**: `userId = 1`

### B) لیست همه Snapshots
**Endpoint**: `GET /api/analytics`  
**Token**: admin

> DataInitializer یک SpendingSnapshot با income=500M و expense=120M ساخته.

---

## گام ۷: تست Audit-Service (8092) — حسابرسی

**سرویس**: Audit-Service (8092)

### A) لیست Audit Logs
**Endpoint**: `GET /api/audit`  
**Token**: admin

### B) Audit Logs یک کاربر
**Endpoint**: `GET /api/audit/user/{userId}`  
**Path variable**: `userId = 1`

> DataInitializer یک log با action=SYSTEM_STARTUP ساخته.

---

## 📋 خلاصه ترتیب تست سریع

| ترتیب | سرویس | Endpoint | توضیح |
|-------|-------|----------|-------|
| 1 | Monolith 8081 | `POST /api/auth/login` | لاگین + گرفتن Token |
| 2 | Monolith 8081 | `GET /api/users` | تست JWT (Admin) |
| 3 | Monolith 8081 | `GET /api/accounts` | باید 1 حساب seed ببینید |
| 4 | Monolith 8081 | `POST /api/accounts/1/deposit?amount=50000000` | واریز |
| 5 | Monolith 8081 | `GET /api/cards` | باید 1 کارت seed ببینید |
| 6 | Monolith 8081 | `GET /api/loans` | باید 1 وام PENDING ببینید |
| 7 | Monolith 8081 | `POST /api/loans/1/approve` | تایید وام (Admin) |
| 8 | Monolith 8081 | `GET /api/notifications` | باید نوتیفیکیشن seed ببینید |
| 9 | TX-Write 8088 | `POST /api/transactions` | ایجاد تراکنش |
| 10 | TX-Write 8088 | `POST /api/transactions/1/complete` | کامل کردن تراکنش |
| 11 | TX-Read 8087 | `GET /api/transactions` | باید تراکنش CQRS را ببینید |
| 12 | Fraud 8091 | `GET /api/fraud` | باید FraudAlert seed ببینید |
| 13 | Analytics 8093 | `GET /api/analytics` | باید SpendingSnapshot ببینید |
| 14 | Audit 8092 | `GET /api/audit` | باید AuditLog seed ببینید |

---

## ⚠️ نکات مهم

1. **Token یکسان در همه سرویس‌ها**: JWT توسط Monolith صادر می‌شود ولی در همه سرویس‌ها (چون secret یکسان است) قابل validate است. در Swagger هر سرویس، همان `Bearer <token>` را وارد کنید.

2. **اگر 403 گرفتید**: یعنی role شما اجازه ندارد. مثلاً `GET /api/users` فقط `ADMIN` دارد. با `admin` لاگین کنید.

3. **اگر 500 در TX-Write دیدید**: Monolith (8081) باید بالا باشد. TX-Write برای balance check به Monolith `http://localhost:8081/internal/accounts/{id}/balance` call می‌زند.

4. **RabbitMQ**: اگر eventها نمی‌رسند (مثلاً TX-Read خالی است)، RabbitMQ را چک کنید: http://localhost:15672 (guest/guest)

5. **اگر GET /api/accounts خطای 500 داد**: دیتابیس Monolith (`banking_monolith_db`) را چک کنید — جدول `accounts` باید ساخته شده باشد (`ddl-auto: update`).
