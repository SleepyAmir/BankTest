# 🗺️ Entity Placement Guide — Banking Hybrid Architecture

## فایل‌ها کجا قرار گرفتن؟

| فایل | مسیر نهایی | ماژول/سرویس |
|------|-----------|-------------|
| `BaseEntity.java` | `shared-kernel/src/main/java/com/springbank/common/entity/BaseEntity.java` | Shared Kernel (کتابخانه مشترک) |
| `User.java` | `banking-monolith/src/main/java/com/springbank/user/entity/User.java` | Monolith — User Module |
| `Role.java` | `banking-monolith/src/main/java/com/springbank/user/entity/Role.java` | Monolith — User Module |
| `Permission.java` | `banking-monolith/src/main/java/com/springbank/user/entity/Permission.java` | Monolith — User Module |
| `Token.java` | `banking-monolith/src/main/java/com/springbank/user/entity/Token.java` | Monolith — User Module |
| `OtpVerification.java` | `banking-monolith/src/main/java/com/springbank/user/entity/OtpVerification.java` | Monolith — User Module |
| `UserDevice.java` | `banking-monolith/src/main/java/com/springbank/user/entity/UserDevice.java` | Monolith — User Module |
| `KycVerification.java` | `banking-monolith/src/main/java/com/springbank/user/entity/KycVerification.java` | Monolith — User Module |
| `Account.java` | `banking-monolith/src/main/java/com/springbank/account/entity/Account.java` | Monolith — Account Module |
| `Branch.java` | `banking-monolith/src/main/java/com/springbank/account/entity/Branch.java` | Monolith — Account Module |
| `ExchangeRate.java` | `banking-monolith/src/main/java/com/springbank/account/entity/ExchangeRate.java` | Monolith — Account Module |
| `OpenBankingConsent.java` | `banking-monolith/src/main/java/com/springbank/account/entity/OpenBankingConsent.java` | Monolith — Account Module |
| `Card.java` | `banking-monolith/src/main/java/com/springbank/card/entity/Card.java` | Monolith — Card Module |
| `Loan.java` | `banking-monolith/src/main/java/com/springbank/loan/entity/Loan.java` | Monolith — Loan Module |
| `LoanInstallment.java` | `banking-monolith/src/main/java/com/springbank/loan/entity/LoanInstallment.java` | Monolith — Loan Module |
| `CreditScore.java` | `banking-monolith/src/main/java/com/springbank/loan/entity/CreditScore.java` | Monolith — Loan Module |
| `Notification.java` | `banking-monolith/src/main/java/com/springbank/notification/entity/Notification.java` | Monolith — Notification Module |
| `AuditLog.java` | `audit-service/src/main/java/com/springbank/audit/entity/AuditLog.java` | Audit Microservice |
| `Transaction.java` (write) | `transaction-write/src/main/java/com/springbank/transaction/write/entity/Transaction.java` | Transaction Write |
| `Transaction.java` (read) | `transaction-read/src/main/java/com/springbank/transaction/read/entity/Transaction.java` | Transaction Read (CQRS) |
| `FraudAlert.java` | `fraud-service/src/main/java/com/springbank/fraud/entity/FraudAlert.java` | Fraud Detection |
| `AmlAlert.java` | `fraud-service/src/main/java/com/springbank/fraud/entity/AmlAlert.java` | Fraud Detection |
| `SpendingSnapshot.java` | `analytics-service/src/main/java/com/springbank/analytics/entity/SpendingSnapshot.java` | Analytics |

---

## ⚠️ نکات بسیار مهم (حتماً بخون!)

### ۱. shared-kernel (کتابخانه مشترک)
- `BaseEntity` و `enums` های مشترک (مثل `AccountStatus`, `TransactionStatus`, `LoanStatus`...) باید اینجا باشن.
- همه سرویس‌ها به `shared-kernel` وابستگی دارن (`dependency` توی POM).
- **این enums هنوز آپلود نشدن** — باید دستی بسازی‌شون یا آپلود کنی.

### ۲. CQRS — دو نسخه Transaction
- `transaction-write` و `transaction-read` هر کدوم یه نسخه `Transaction` دارن.
- نسخه read می‌تونه ساده‌تر باشه (فقط فیلدهایی که توی query لازمه).
- نسخه write باید event publish کنه بعد از save.

### ۳. 🔴 مشکل جدی: رابطه (Relation) بین سرویس‌های مختلف

توی Entity های فعلی، رابطه JPA مستقیم بین سرویس‌های مختلف وجود داره:

```java
// ❌ Transaction (microservice) -> Account (monolith)
@ManyToOne private Account fromAccount;

// ❌ FraudAlert (microservice) -> Transaction (microservice دیگه) + User (monolith)
@OneToOne  private Transaction transaction;
@ManyToOne private User user;
```

این خلاف **Golden Rule** معماریه:
> "Never JOIN across service databases. Use IDs and fetch separately."

#### ✅ راه حل: تبدیل Entity Reference به Long ID

| الان (غلط) | باید باشه (درست) |
|-----------|-----------------|
| `private Account fromAccount;` | `private Long fromAccountId;` |
| `private User user;` | `private Long userId;` |
| `private Transaction transaction;` | `private Long transactionId;` |
| `private Card card;` | `private Long cardId;` |
| `private Loan loan;` | `private Long loanId;` |
| `private LoanInstallment loanInstallment;` | `private Long loanInstallmentId;` |
| `List<Transaction> outgoingTransactions;` | `حذف شه` (متعلق به سرویس دیگه‌ست) |
| `List<FraudAlert> amlAlerts;` | `حذف شه` |

**تنها استثناء:** داخل Monolith می‌تونی رابطه داشته باشی (مثلاً `Loan` -> `Account` یا `User` -> `Role`) چون توی یه دیتابیس و یه JVM هستن.

### ۴. 🔴 مشکل R2DBC vs JPA

- `transaction-read` و `fraud-service` توی POM شون `R2DBC` دارن (برای WebFlux Reactive).
- ولی Entity هایی که گذاشتی از JPA هستن (`@Entity`, `@ManyToOne`, ...).
- **R2DBC از JPA پشتیبانی نمی‌کنه.**

#### راه حل‌های پیشنهادی:
**الف) ساده‌تر:** تغییر `transaction-read` و `fraud-service` به JPA
- توی `pom.xml` این دو تا:
  - حذف: `r2dbc-postgresql` و `spring-boot-starter-data-r2dbc`
  - اضافه: `spring-boot-starter-data-jpa` و `postgresql` (runtime)
- اینطوری همه Entity ها بدون تغییر کار می‌کنن.
- **توجه:** اینطوری دیگه خیلی "Reactive" نیستن، ولی برای شروع بهتره.

**ب) حرفه‌ای‌تر:** تبدیل Entity های این دو سرویس به R2DBC
- جایگزینی `@Entity` با `@Table`
- جایگزینی `@Id` با `@Id` از `org.springframework.data.annotation.Id`
- حذف `@ManyToOne`, `@OneToOne`, `@OneToMany`
- استفاده از `ReactiveCrudRepository` به جای `JpaRepository`
- نیاز به دانش Spring WebFlux / Mono / Flux داره.

> 💡 **توصیه من برای شروع:** گزینه **الف** (JPA) رو انتخاب کن تا پروژه کامپایل و اجرا بشه. بعداً می‌تونی reactive کنی.

### ۵. AuditLog (✅ درست طراحی شده)
- `AuditLog` فقط `Long entityId` ذخیره می‌کنه، نه reference به Entity خارجی.
- این دقیقاً همونیه که architecture guide می‌گه: **Store IDs only.**

### ۶. Notification (❌ نیاز به ریفکتور)
- `Notification` داخل Monolith هست ولی `Transaction` و `Loan` رو به صورت entity reference می‌گیره.
- بهتره بشه: `Long transactionId`, `Long loanId`.
- چون ممکنه event از microservice بیاد (مثلاً transaction.completed) و notification توی monolith ذخیره شه.

---

## 🎯 جدول دیتابیس‌ها (Schema) بر اساس معماری

| سرویس | دیتابیس | تیبل‌ها |
|-------|---------|--------|
| Monolith | `banking_monolith_db` | `usr_users`, `usr_roles`, `usr_tokens`, `usr_otp`, `usr_devices`, `usr_kyc`, `acc_accounts`, `acc_branches`, `acc_rates`, `crd_cards`, `ln_loans`, `ln_installments`, `ln_credit_scores`, `ntf_notifications`, `obc_consents` |
| Audit | `banking_audit_db` | `audit_logs` |
| Transaction | `banking_transaction_db` | `txn_transactions` |
| Fraud | `banking_fraud_db` | `frd_fraud_alerts`, `frd_aml_alerts` |
| Analytics | `banking_analytics_db` | `anl_spending_snapshots` |

---

## 🛠️ گام بعدی برای شما

1. **Enum ها** رو توی `shared-kernel/src/main/java/com/springbank/common/enums/` بساز.
2. **ریفکتور cross-service relations** (تبدیل entity ref به Long ID).
3. **تصمیم بگیر** R2DBC یا JPA برای `transaction-read` و `fraud-service`.
4. **Application class** (مثل `BankingMonolithApplication.java`) برای هر ماژول بساز.
5. **Repository interface** ها رو بساز (`JpaRepository<Entity, Long>`).
6. **application.yml** برای هر سرویس بنویس (پورت، دیتابیس، RabbitMQ).
