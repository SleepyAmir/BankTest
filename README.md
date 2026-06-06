# Banking Hybrid Architecture — راهنمای جامع اجرا

## پیش‌نیازها

- **Java 21** (JDK)
- **Maven 3.9+**
- **Docker & Docker Compose** (برای روش اول)
- **IntelliJ IDEA** (ترجیحاً Ultimate برای Spring پشتیبانی بهتر)
- **PostgreSQL 16** (اگر Docker نمی‌خواهید)
- **Redis 7** (اگر Docker نمی‌خواهید)
- **RabbitMQ 3** (اگر Docker نمی‌خواهید)

---

## ساختار پروژه

```
banking-hybrid-architecture/
├── shared-kernel/          # موجودیت‌ها، enum‌ها، event‌ها، exception‌ها، repository/service پایه
├── api-gateway/            # درگاه ورودی (port 8090)
├── banking-monolith/       # مونولیت هسته (port 8081) — User, Account, Card, Loan, Notification
├── transaction-write/      # نوشتن تراکنش (port 8088) — CQRS Write Model
├── transaction-read/       # خواندن تراکنش (port 8087) — CQRS Read Model + RabbitMQ Consumer
├── fraud-service/          # تشخیص تقلب (port 8091) — RabbitMQ Consumer
├── analytics-service/      # تحلیل‌ها (port 8093) — RabbitMQ Consumer + Scheduling
├── audit-service/          # لاگ حسابرسی (port 8092) — RabbitMQ Consumer
├── docker-compose.yml      # همه سرویس‌ها و زیرساخت‌ها
└── pom.xml                 # Root Maven POM
```

---

## پورت‌های سرویس‌ها

| سرویس | پورت | توضیحات |
|-------|------|---------|
| API Gateway | **8090** | درگاه ورودی اصلی (قبلاً 8080 بود برای جلوگیری از conflict) |
| Banking Monolith | 8081 | هسته سیستم: احراز هویت، حساب، کارت، وام، نوتیفیکیشن |
| Transaction-Read | 8087 | مدل خواندن تراکنش (CQRS) |
| Transaction-Write | 8088 | مدل نوشتن تراکنش + انتشار event |
| Fraud Service | 8091 | تحلیل تقلب و AML |
| Audit Service | 8092 | ذخیره‌سازی لاگ حسابرسی |
| Analytics Service | 8093 | تحلیل رفتار و snapshot هزینه |
| PostgreSQL Monolith | 5432 | دیتابیس مونولیت |
| PostgreSQL Transaction | 5433 | دیتابیس تراکنش |
| PostgreSQL Fraud | 5434 | دیتابیس تقلب |
| PostgreSQL Analytics | 5435 | دیتابیس تحلیل |
| PostgreSQL Audit | 5436 | دیتابیس حسابرسی |
| Redis | 6379 | کش و session |
| RabbitMQ | 5672 / 15672 | Message Broker (Management UI: guest/guest) |

---

## روش ۱: اجرا با Docker Compose (بسیار ساده)

### ۱. باز کردن پروژه در IntelliJ

```bash
File → Open → banking-hybrid-architecture
```

IntelliJ خودش Maven wrapper را تشخیص می‌دهد. یک بار `Maven → Reload Project` بزنید.

### ۲. بیلد کل پروژه

```bash
cd banking-hybrid-architecture
mvn clean install -DskipTests
```

یا از داخل IntelliJ: `Maven tool window → banking-hybrid-architecture → Lifecycle → clean` سپس `install`

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

### ۵. تست API ها

| سرویس | Swagger UI |
|-------|------------|
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

اگر Docker نصب دارید ولی نمی‌خواهید کل پروژه را با Docker اجرا کنید، فقط زیرساخت‌ها را Docker ای اجرا کنید:

```bash
docker run -d --name postgres-monolith \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_monolith_db \
  -p 5432:5432 postgres:16-alpine

docker run -d --name postgres-transaction \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_transaction_db \
  -p 5433:5432 postgres:16-alpine

docker run -d --name redis \
  -p 6379:6379 redis:7-alpine

docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management-alpine
```

> **نکته:** برای سایر دیتابیس‌ها (fraud, analytics, audit) هم همین کار را با پورت‌های 5434 تا 5436 تکرار کنید.

### ۲. بیلد shared-kernel اول

```bash
mvn clean install -pl shared-kernel
```

### ۳. اجرای سرویس‌ها به ترتیب

| ترتیب | سرویس | دستور |
|-------|-------|-------|
| ۱ | **shared-kernel** | `mvn install -pl shared-kernel` (فقط بیلد) |
| ۲ | **banking-monolith** | `mvn spring-boot:run -pl banking-monolith` |
| ۳ | **transaction-write** | `mvn spring-boot:run -pl transaction-write` |
| ۴ | **transaction-read** | `mvn spring-boot:run -pl transaction-read` |
| ۵ | **fraud-service** | `mvn spring-boot:run -pl fraud-service` |
| ۶ | **analytics-service** | `mvn spring-boot:run -pl analytics-service` |
| ۷ | **audit-service** | `mvn spring-boot:run -pl audit-service` |
| ۸ | **api-gateway** | `mvn spring-boot:run -pl api-gateway` |

از داخل IntelliJ هم می‌توانید هر `Application.java` را Run کنید.

---

## تنظیمات مهم

### `application.yml` در سرویس‌ها

- `ddl-auto: update` — جداول خودکار ساخته می‌شوند (فقط برای dev)
- `spring.threads.virtual.enabled: true` — Virtual Threads فعال است
- JWT Secret از `jwt.secret` در properties خوانده می‌شود (هاردکد نشده)

### اتصال transaction-write به monolith

در `transaction-write/src/main/resources/application.yml`:
```yaml
services:
  monolith:
    url: http://localhost:8081   # در Docker: http://banking-monolith:8081
```

این برای چک کردن موجودی حساب قبل از انتقال وجه استفاده می‌شود.

---

## جریان کاری End-to-End (E2E)

برای اینکه جریان کامل بانکی کار کند، این مراحل را دنبال کنید:

### ۱. ثبت‌نام کاربر
```
POST http://localhost:8090/api/auth/register
```
(یا مستقیم به Monolith: 8081)

### ۲. ورود و دریافت JWT Token
```
POST http://localhost:8090/api/auth/login
```

### ۳. ایجاد حساب بانکی
```
POST http://localhost:8090/api/accounts
Header: Authorization: Bearer <token>
```

### ۴. واریز وجه
```
POST http://localhost:8090/api/accounts/{id}/deposit
```

### ۵. ایجاد کارت
```
POST http://localhost:8090/api/cards
```

### ۶. ایجاد تراکنش (انتقال وجه)
```
POST http://localhost:8090/api/transactions
Body: { fromAccountId, toAccountId, amount, type: TRANSFER }
```

اتفاقات پس از تراکنش:
- `transaction-write` تراکنش را ذخیره می‌کند
- event `TransactionCompletedEvent` به RabbitMQ ارسال می‌شود
- `transaction-read` event را گرفته و در read DB ذخیره می‌کند (CQRS)
- `fraud-service` event را گرفته و تحلیل تقلب انجام می‌دهد
- `audit-service` event را گرفته و لاگ حسابرسی می‌نویسد
- `analytics-service` event را گرفته و snapshot هزینه را به‌روز می‌کند
- `banking-monolith` (notification) event را گرفته و نوتیفیکیشن می‌سازد

### ۷. درخواست وام
```
POST http://localhost:8090/api/loans
```

پس از تایید وام، `LoanApprovedEvent` ارسال شده و نوتیفیکیشن به کاربر می‌رسد.

---

## نکات مهم برای توسعه‌دهنده

### ۱. Business Logic خالی است
این پروژه یک **اسکلت کامل** است. متدهای Service مثل:
- `transferMoney()`
- `approveLoan()`
- `deposit()`
- `withdraw()`

فقط signature و wiring (Autowired, Transactional, EventListener) دارند و **logic داخلی خالی/placeholder** است. برای کامل کردن پروژه، باید این متدها را پیاده‌سازی کنید.

### ۲. Reactive/WebFlux
در معماری PDF اصلی، `transaction-read` و `fraud-service` باید Reactive (R2DBC) باشند، اما در این اسکلت برای سادگی **JPA + Tomcat** استفاده شده. اگر می‌خواهید Reactive کنید، باید dependencyها و entityها را تغییر دهید.

### ۳. Cross-Service Entity References
در microserviceها، به جای `@ManyToOne` به entity دیگر، فقط `Long ...Id` نگهداری می‌شود (مثلاً `fromAccountId` به جای `Account account`). این از coupling بین سرویس‌ها جلوگیری می‌کند.

### ۴. Security
- `SecurityConfig` در monolith: JWT, CORS, CSP, HSTS, XSS Protection
- `JwtAuthenticationFilter` از هدر `Authorization` و پارامتر `?token=` پشتیبانی می‌کند (برای SSE)
- Gateway فقط route می‌کند؛ احراز هویت در monolith انجام می‌شود

### ۵. AOP
- `AuditAspect` — ثبت لاگ عملیات
- `LoggingAspect` — لاگ ورود/خروج متدها
- `PerformanceAspect` — اندازه‌گیری زمان اجرا
- `ValidationAspect` — چک validation
- `EncryptionAspect` — رمزنگاری فیلدها

---

## عیب‌یابی

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
از RabbitMQ Management UI در `http://localhost:15672` بررسی کنید که queueها ساخته شده‌اند.

---

## توسعه‌دهنده

این پروژه بر اساس معماری Hybrid (Modular Monolith + Microservices) طراحی شده و آماده پیاده‌سازی Business Logic شماست.
