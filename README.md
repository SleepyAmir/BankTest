# Banking Hybrid Architecture — راهنمای جامع اجرا (نسخه نهایی)

> ⚠️ **توجه**: این نسخه، نسخه **کامل و پیاده‌سازی‌شده** است. Business Logic داخل متدها نوشته شده و قابل اجراست.
> برای گزارش خطا، به لاگ‌های کنسول نگاه کنید — هر سرویس marker‌های `[SVC-ACTION]` دارد.

---

## پیش‌نیازها

- **Java 21** (JDK)
- **Maven 3.9+**
- **Docker & Docker Compose** (روش پیشنهادی)
- **IntelliJ IDEA** (Ultimate یا Community — هر دو پشتیبانی Spring دارند)
- **PostgreSQL 16** / **Redis 7** / **RabbitMQ 3** (اگر Docker نمی‌خواهید)

---

## ساختار پروژه

```
banking-hybrid-architecture/
├── shared-kernel/          # موجودیت‌ها، enum‌ها، event‌ها، exception‌ها، repository/service پایه
├── api-gateway/            # درگاه ورودی (port 8090) ← قبلاً 8080 بود، برای جلوگیری از conflict تغییر کرد
├── banking-monolith/       # مونولیت هسته (port 8081) — User, Account, Card, Loan, Notification, Security
├── transaction-write/        # نوشتن تراکنش (port 8088) — CQRS Write + RestTemplate به Monolith
├── transaction-read/         # خواندن تراکنش (port 8087) — CQRS Read + RabbitMQ Consumer + Caching
├── fraud-service/          # تشخیص تقلب (port 8091) — RabbitMQ Consumer + Rule Engine
├── analytics-service/      # تحلیل‌ها (port 8093) — RabbitMQ Consumer + Scheduling + Aggregation
├── audit-service/          # لاگ حسابرسی (port 8092) — RabbitMQ Consumer
├── docker-compose.yml      # همه سرویس‌ها و زیرساخت‌ها
└── pom.xml                 # Root Maven POM
```

---

## پورت‌های سرویس‌ها

| سرویس | پورت | توضیحات |
|-------|------|---------|
| API Gateway | **8090** | درگاه ورودی اصلی (برای جلوگیری از conflict با 8080 تغییر کرد) |
| Banking Monolith | 8081 | هسته: احراز هویت، حساب، کارت، وام، نوتیفیکیشن، SSE |
| Transaction-Read | 8087 | مدل خواندن تراکنش (CQRS) + Redis Cache |
| Transaction-Write | 8088 | مدل نوشتن تراکنش + انتشار event |
| Fraud Service | 8091 | تحلیل تقلب (مبلغ >50M، دستگاه ناشناس) و AML |
| Audit Service | 8092 | ذخیره‌سازی لاگ حسابرسی |
| Analytics Service | 8093 | تحلیل رفتار و SpendingSnapshot |
| PostgreSQL Monolith | 5432 | دیتابیس مونولیت |
| PostgreSQL Transaction | 5433 | دیتابیس تراکنش |
| PostgreSQL Fraud | 5434 | دیتابیس تقلب |
| PostgreSQL Analytics | 5435 | دیتابیس تحلیل |
| PostgreSQL Audit | 5436 | دیتابیس حسابرسی |
| Redis | 6379 | کش و session |
| RabbitMQ | 5672 / 15672 | Message Broker (Management UI: guest/guest) |

---

## ✅ فیکس‌های اعمال‌شده (Critical Fixes)

1. **Conflict پورت 8080** → Gateway به **8090** منتقل شد. CORS در `SecurityConfig` هم `http://localhost:8090` را اضافه کردیم.
2. **RabbitMQ `SimpleRabbitListenerContainerFactory` Bean Conflict** → در همه سرویس‌ها به `RabbitListenerContainerFactoryCustomizer<SimpleRabbitListenerContainerFactory>` تغییر کرد. دیگر با auto-configuration اسپرینگ تداخل ندارد.
3. **BaseEntity Soft-Delete** → `@PreUpdate` حذف شد؛ `@PreRemove` روی `onRemove()` نگه داشته شد. حذف نرم از طریق `save()` با `deleted=true` کار می‌کند.
4. **Cross-Service Relations** → همه `ManyToOne` بین سرویس‌ها به `Long ...Id` تبدیل شدند.
5. **TransactionWrite → Monolith** → `RestTemplateConfig` اضافه شد. Balance Check و Update Balance از طریق `RestTemplate` به `http://localhost:8081/internal/accounts/...` انجام می‌شود.
6. **Notification Consumer** → `@RabbitListener` روی کلاس + `@RabbitHandler` برای هر event type. `@RabbitHandler(isDefault = true)` برای unknown events.
7. **Enum Type Safety** → `FraudAlertRepository.findByRiskLevel(FraudRiskLevel)` و `AmlAlertRepository.findByStatus(AlertStatus)` اصلاح شدند (دیگر String نیستند).
8. **SPEL N+1** → `@PreAuthorize` در `CardController`, `LoanController`, `AccountController` از `getById().userId()` استفاده می‌کند (DTO) به جای لود کردن entity graph.
9. **Virtual Threads** → `spring.threads.virtual.enabled=true` در همه سرویس‌ها.
10. **JWT Secret** → از `application.yml` (env) خوانده می‌شود؛ هاردکد نیست.
11. **Logging Markers** → هر سرویس marker منحصربه‌فرد دارد (مثلاً `[TX-CREATE]`, `[FRAUD-ANALYZE]`, `[LOAN-APPROVE]`).
12. **Application Startup Banner** → همه `Application.java` بعد از `ContextRefreshedEvent` پورت و dependencyها را لاگ می‌کنند.
13. **ddl-auto: update** → در dev جداول خودکار ساخته می‌شوند.
14. **Reactive (WebFlux/R2DBC)** → در این نسخه **JPA + Tomcat** استفاده شده تا سازگاری با entity فایل‌های شما حفظ شود. PDF اصلی reactive را پیشنهاد داده بود اما این پروژه blocking JPA است.

---

## روش ۱: اجرا با Docker Compose (بسیار ساده — پیشنهادی)

### ۱. باز کردن در IntelliJ

```bash
File → Open → banking-hybrid-architecture
```

IntelliJ خودش Maven را تشخیص می‌دهد. یک بار `Maven → Reload Project` بزنید.

### ۲. بیلد کل پروژه

```bash
cd banking-hybrid-architecture
mvn clean install -DskipTests
```

یا از داخل IntelliJ: `Maven tool window → Lifecycle → clean` سپس `install`.

> **نکته**: `shared-kernel` ابتدا بیلد می‌شود و در local Maven repo قرار می‌گیرد. سایر سرویس‌ها به آن dependency دارند.

### ۳. اجرا با Docker Compose

```bash
docker-compose up -d
```

این دستور همه این موارد را همزمان بالا می‌آورد:
- ۵ دیتابیس PostgreSQL
- Redis
- RabbitMQ
- ۷ سرویس Spring Boot

### ۴. بررسی وضعیت

```bash
docker-compose ps
docker-compose logs -f banking-monolith
```

### ۵. تست API ها (Swagger UI)

| سرویس | آدرس Swagger |
|-------|--------------|
| Monolith | http://localhost:8081/swagger-ui.html |
| Transaction-Write | http://localhost:8088/swagger-ui.html |
| Transaction-Read | http://localhost:8087/swagger-ui.html |
| Fraud | http://localhost:8091/swagger-ui.html |
| Analytics | http://localhost:8093/swagger-ui.html |
| Audit | http://localhost:8092/swagger-ui.html |
| Gateway | http://localhost:8090 |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |

### ۶. توقف همه

```bash
docker-compose down
```

برای پاک کردن دیتا هم:
```bash
docker-compose down -v
```

---

## روش ۲: اجرا لوکال بدون Docker (برای توسعه)

### ۱. بالا آوردن زیرساخت‌ها دستی

اگر Docker دارید ولی نمی‌خواهید کل پروژه را Docker ای اجرا کنید، فقط زیرساخت‌ها را Docker ای بیاورید:

```bash
# PostgreSQL Monolith
docker run -d --name postgres-monolith \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_monolith_db \
  -p 5432:5432 postgres:16-alpine

# PostgreSQL Transaction
docker run -d --name postgres-transaction \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_transaction_db \
  -p 5433:5432 postgres:16-alpine

# PostgreSQL Fraud
docker run -d --name postgres-fraud \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_fraud_db \
  -p 5434:5432 postgres:16-alpine

# PostgreSQL Analytics
docker run -d --name postgres-analytics \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_analytics_db \
  -p 5435:5432 postgres:16-alpine

# PostgreSQL Audit
docker run -d --name postgres-audit \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_audit_db \
  -p 5436:5432 postgres:16-alpine

# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management-alpine
```

### ۲. بیلد shared-kernel

```bash
mvn clean install -pl shared-kernel
```

### ۳. اجرای سرویس‌ها به ترتیب

| ترتیب | سرویس | دستور / IntelliJ Run |
|-------|-------|----------------------|
| ۱ | **shared-kernel** | `mvn install -pl shared-kernel` (فقط بیلد) |
| ۲ | **banking-monolith** | `mvn spring-boot:run -pl banking-monolith` |
| ۳ | **transaction-write** | `mvn spring-boot:run -pl transaction-write` |
| ۴ | **transaction-read** | `mvn spring-boot:run -pl transaction-read` |
| ۵ | **fraud-service** | `mvn spring-boot:run -pl fraud-service` |
| ۶ | **analytics-service** | `mvn spring-boot:run -pl analytics-service` |
| ۷ | **audit-service** | `mvn spring-boot:run -pl audit-service` |
| ۸ | **api-gateway** | `mvn spring-boot:run -pl api-gateway` |

یا از داخل IntelliJ: هر `*Application.java` را با کلیک راست → `Run` اجرا کنید.

---

## جریان کاری End-to-End (E2E) — نمونه تست کامل

### ۱. ثبت‌نام کاربر
```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!","email":"test@test.com","firstName":"Ali","lastName":"Ahmadi","phoneNumber":"09123456789"}'
```
(یا مستقیم به Monolith: 8081)

### ۲. ورود و دریافت JWT Token
```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!"}'
```

### ۳. ایجاد حساب بانکی
```bash
curl -X POST http://localhost:8090/api/accounts \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"accountNumber":"IR123456789012345678901234","type":"CHECKING","alias":"Main Account"}'
```

### ۴. واریز وجه
```bash
curl -X POST http://localhost:8090/api/accounts/1/deposit?amount=100000000 \
  -H "Authorization: Bearer <TOKEN>"
```

### ۵. ایجاد کارت
```bash
curl -X POST http://localhost:8090/api/cards \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"cardNumber":"6104331234567890","cvv2":"123","pin":"1234","expirationDate":"2028-12-31"}'
```

### ۶. ایجاد تراکنش (انتقال وجه)
```bash
curl -X POST http://localhost:8090/api/transactions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":50000000,"type":"TRANSFER","currency":"IRR"}'
```

**اتفاقات پس از تراکنش:**
1. `transaction-write` تراکنش را با status=PENDING ذخیره می‌کند.
2. `TransactionCompletedEvent` به RabbitMQ (exchange `banking.exchange`, routing `transaction.completed`) ارسال می‌شود.
3. `transaction-read` event را گرفته و در read DB ذخیره می‌کند (CQRS).
4. `fraud-service` event را گرفته و rule engine اجرا می‌کند (مبلغ >50M → FraudAlert + AML).
5. `audit-service` event را گرفته و `AuditLog` می‌نویسد.
6. `analytics-service` event را گرفته و `SpendingSnapshot` را به‌روز می‌کند.
7. `banking-monolith` (notification) event را گرفته و `Notification` entity + SSE push می‌سازد.

### ۷. درخواست وام
```bash
curl -X POST http://localhost:8090/api/loans \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"accountId":1,"amount":1000000000,"durationMonths":12,"purpose":"House Renovation"}'
```

پس از تایید وام:
```bash
curl -X POST http://localhost:8090/api/loans/1/approve?approvedBy=admin \
  -H "Authorization: Bearer <TOKEN>"
```

- `LoanApprovedEvent` ارسال می‌شود.
- نوتیفیکیشن به کاربر می‌رسد.
- جدول اقساط (Installments) با فرمول PMT ساخته می‌شود.

---

## نکات مهم توسعه

### ۱. Business Logic پیاده‌سازی شده
این پروژه **فقط اسکلت نیست**. متدهای زیر کامل پیاده‌سازی شده‌اند:
- `AccountWriteService.deposit()`, `withdraw()`, `createAccount()` — با balance tracking و event publishing.
- `LoanWriteService.createLoan()`, `approveLoan()`, `rejectLoan()` — با تولید اقساط PMT و `LoanApprovedEvent`.
- `TransactionWriteService.createTransaction()`, `completeTransaction()`, `reverseTransaction()`, `failTransaction()` — با balance check و update از طریق RestTemplate به Monolith.
- `FraudAnalysisService.analyzeTransaction()` — Rule engine (Large TX, Unknown Device) + AML auto-generation.
- `AnalyticsService` — Income/expense aggregation + `SpendingSnapshot`.
- `AuditLogService` — Persistence از `AuditLogEvent`.
- `NotificationEventConsumer` — Create Notification + SSE broadcast.
- `InstallmentReminderJob` — Scheduled 9 AM برای اقساط overdue/upcoming.
- `AuthController.refresh()` — با لود user از `CustomUserDetailsService` و صدور token جدید.
- `InternalController` — `/internal/accounts/{id}/balance`, `/deposit`, `/withdraw` برای inter-service calls.

### ۲. Reactive/WebFlux
در معماری PDF اصلی، `transaction-read` و `fraud-service` باید Reactive (R2DBC) باشند، اما در این نسخه برای سازگاری با entity فایل‌های JPA شما، **JPA + Tomcat** استفاده شده. اگر می‌خواهید Reactive کنید، dependencyها و entityها را به R2DBC تغییر دهید.

### ۳. Cross-Service Entity References
در microserviceها، به جای `@ManyToOne` به entity دیگر، فقط `Long ...Id` نگهداری می‌شود (مثلاً `Transaction.fromAccountId` به جای `Account account`). این از coupling بین سرویس‌ها جلوگیری می‌کند.

### ۴. Security
- `SecurityConfig` در monolith: JWT, CORS, CSP, HSTS, XSS Protection.
- `JwtAuthenticationFilter` از هدر `Authorization` و پارامتر `?token=` پشتیبانی می‌کند (برای SSE).
- Gateway فقط route می‌کند؛ احراز هویت اصلی در monolith انجام می‌شود.
- `GlobalExceptionHandler` همه خطاها را catch و با `ApiResponse` استاندارد برمی‌گرداند.

### ۵. AOP
- `AuditAspect` — ثبت لاگ عملیات روی `@Auditable`.
- `LoggingAspect` — لاگ ورود/خروج متدها.
- `PerformanceAspect` — اندازه‌گیری زمان اجرا.
- `ValidationAspect` — چک validation.
- `EncryptionAspect` — hook برای رمزنگاری فیلدها.

---

## عیب‌یابی سریع (Troubleshooting)

### پورت 8080 درگیری دارد؟
Gateway الان روی **8090** است. اگر باز هم conflict دارید، در `api-gateway/src/main/resources/application.yml` تغییر دهید.

### `shared-kernel` پیدا نمی‌شود؟
```bash
mvn clean install -pl shared-kernel
```
را قبل از اجرای سایر سرویس‌ها حتماً بزنید.

### جداول ساخته نمی‌شوند؟
در `application.yml` هر سرویس، `ddl-auto: update` تنظیم شده. اگر کار نکرد، دیتابیس‌ها را یکبار دستی بسازید.

### RabbitMQ message نمی‌رسد؟
از RabbitMQ Management UI در `http://localhost:15672` بررسی کنید که queueها ساخته شده‌اند. queueهای مورد نیاز:
- `audit.queue`
- `notification.queue`
- `fraud.queue`
- `analytics.queue`
- `transaction.read.queue`

### تراکنش balance check خطا می‌دهد؟
بررسی کنید `banking-monolith` روی پورت 8081 بالا باشد. `transaction-write` با `RestTemplate` به `http://localhost:8081/internal/accounts/{id}/balance` call می‌زند.
لاگ `[TX-REST]` را چک کنید.

### خطای `OptimisticLockException`؟
در `BaseEntityRepository` soft-delete با retry (3 بار) پیاده‌سازی شده. اگر همچنان رخ داد، concurrent access وجود دارد.

### `SimpleRabbitListenerContainerFactory` import error؟
این import در فایل‌های `RabbitMQConfig` استفاده می‌شود **فقط** به عنوان type parameter در `RabbitListenerContainerFactoryCustomizer<SimpleRabbitListenerContainerFactory>`. دیگر `@Bean` از نوع `SimpleRabbitListenerContainerFactory` تعریف نمی‌شود و با auto-configuration Spring Boot تداخل ندارد. این **هیچ خطایی نیست** و اگر IDE زرد نشان می‌دهد، می‌توانید ignore کنید یا با `SuppressWarnings("unused")` ساکت کنید.

---

## توسعه‌دهنده

این پروژه بر اساس معماری Hybrid (Modular Monolith + Microservices) طراحی شده و آماده تست و توسعه شماست.

اگر خطای کامپایل یا runtime مشاهده کردید، marker لاگ را گزارش دهید (مثلاً `[TX-REST]`, `[FRAUD-ANALYZE]`) تا سریع‌تر诊断 شود.
