<div align="center">

# 🏦 SpringBank Hybrid Architecture

**Modular Monolith + Microservices | CQRS | Event-Driven | Domain-Driven Design**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://postgresql.org)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-FF6600?logo=rabbitmq&logoColor=white)](https://rabbitmq.com)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io)
[![Docker](https://img.shields.io/badge/Docker-🐳-2496ED?logo=docker&logoColor=white)](https://docker.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

<p align="center">
  <img src="https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/rainbow.png" alt="divider" width="100%">
</p>

</div>

## 📋 فهرست مطالب

- [🎯 معرفی پروژه](#-معرفی-پروژه)
- [🏗️ معماری سیستم](#-معماری-سیستم)
- [⚡ تکنولوژی‌های کلیدی](#-تکنولوژیهای-کلیدی)
- [🗂️ ساختار پروژه](#-ساختار-پروژه)
- [🚀 شروع سریع](#-شروع-سریع)
- [🔧 راه‌اندازی توسعه (بدون Docker)](#-راهاندازی-توسعه-بدون-docker)
- [🧪 تست API با Swagger](#-تست-api-با-swagger)
- [🔐 امنیت](#-امنیت)
- [📊 CQRS و Event-Driven](#-cqrs-و-event-driven)
- [🐳 Docker Compose](#-docker-compose)
- [📝 لاگ‌گیری و مشاهده‌پذیری](#-لاگگیری-و-مشاهدهپذیری)
- [🤝 مشارکت](#-مشارکت)
- [📜 مجوز](#-مجوز)

---

## 🎯 معرفی پروژه

SpringBank یک **سیستم بانکداری مدرن** است که بر اساس **معماری ترکیبی (Hybrid)** طراحی شده است:

- **Modular Monolith** برای هسته کسب‌وکار (کاربر، حساب، کارت، وام، نوتیفیکیشن)
- **Microservices** برای دامنه‌های بحرانی (تراکنش، تقلب، تحلیل، حسابرسی)
- **CQRS Pattern** برای جداسازی خواندن و نوشتن تراکنش‌ها
- **Event-Driven Architecture** با RabbitMQ برای ارتباط ناهمگام بین سرویس‌ها

> **چرا Hybrid؟** مونولیت مدولار برای توسعه سریع‌تر و تست راحت‌تر، و میکروسرویس‌ها برای مقیاس‌پذیری مستقل دامنه‌های بحرانی.

---

## 🏗️ معماری سیستم

```
┌─────────────────────────────────────────────────────────────┐
│                     🌐 API Gateway (8090)                    │
│         JWT Auth · Rate Limiting · Routing · CORS             │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┬──────────────┐
        │              │              │              │
   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
   │  Monolith │   │  TX-Write │   │  TX-Read │   │  Fraud   │
   │  (8081)   │   │  (8088)   │   │  (8087)  │   │  (8091)  │
   │           │   │           │   │          │   │          │
   │ 👤 User   │   │ ✍️ Write  │   │ 📖 Read  │   │ 🛡️ Detect│
   │ 💳 Account│   │   Model   │   │   Model  │   │   AML    │
   │ 💳 Card   │   │           │   │  (Redis) │   │          │
   │ 🏠 Loan   │   │           │   │          │   │          │
   │ 🔔 Notify │   │           │   │          │   │          │
   └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘
        │              │              │              │
        └──────────────┼──────────────┴──────────────┘
                       │
              ┌────────▼────────┐
              │  🐰 RabbitMQ    │
              │   (5672/15672)  │
              │  Event Broker    │
              └────────┬────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
   │ Analytics│   │  Audit   │   │  Notify  │
   │ (8093)   │   │  (8092)  │   │(Monolith)│
   │ 📊 Report│   │ 📝 Logs   │   │ 📨 Push  │
   │ 📈 Stats │   │ 🔍 Track │   │ 🔊 SSE   │
   └─────────┘   └─────────┘   └─────────┘

┌─────────────────────────────────────────────────────────────┐
│              🗄️ Data Layer (PostgreSQL + Redis)              │
│  Monolith:5432  ·  TX:5433  ·  Fraud:5434  ·  Analytics:5435  ·  Audit:5436  │
│  Redis:6379 (Cache · Session · Rate Limit)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚡ تکنولوژی‌های کلیدی

| لایه | تکنولوژی | کاربرد |
|------|---------|--------|
| **Backend** | Java 21 + Spring Boot 3.2 | Virtual Threads, Native Support |
| **Security** | Spring Security 6 + JWT | Role-Based Access, CSP, HSTS, XSS |
| **Data** | Spring Data JPA + Hibernate 6 | ORM, Auditing, Soft Delete, Optimistic Locking |
| **Database** | PostgreSQL 16 | 5 Instance جداگانه برای هر سرویس |
| **Cache** | Redis 7 | Caching, Rate Limiting, Session Store |
| **Messaging** | RabbitMQ 3 | Event-Driven, CQRS Sync, Fan-out |
| **Gateway** | Spring Cloud Gateway | JWT Filter, Rate Limit, Route, Internal Block |
| **Docs** | OpenAPI 3 + Swagger UI | Interactive API Documentation |
| **Mapper** | MapStruct 1.5 | Compile-Time DTO Mapping |
| **Build** | Maven 3.9 | Multi-Module Build |
| **DevOps** | Docker + Docker Compose | One-Command Infrastructure |

---

## 🗂️ ساختار پروژه

```
banking-hybrid-architecture/
├── 📁 shared-kernel/              # 📦 Entities · Enums · Events · Exceptions · Base Repositories
│   └── com.springbank.common/
│       ├── entity/ · dto/ · event/ · exception/
│       ├── enums/ · annotation/
│       └── repository/ · service/
│
├── 📁 banking-monolith/           # 🏛️ Core Monolith (Port 8081)
│   ├── user/ (Security · JWT · Role · Permission)
│   ├── account/ (Balance · Deposit · Withdraw)
│   ├── card/ (CRUD · PIN · CVV2)
│   ├── loan/ (PMT Formula · Installments · Approval)
│   ├── notification/ (SSE · Push · In-App)
│   ├── config/ (Security · RabbitMQ · Redis · AOP)
│   └── internal/ (Inter-Service REST APIs)
│
├── 📁 api-gateway/               # 🌐 Gateway (Port 8090)
│   └── filter/ (JwtAuth · RateLimit · Logging)
│
├── 📁 transaction-write/           # ✍️ CQRS Write (Port 8088)
│   ├── service/ (Balance Check · Event Publish)
│   └── messaging/ (TransactionCompletedEvent)
│
├── 📁 transaction-read/            # 📖 CQRS Read (Port 8087)
│   ├── consumer/ (RabbitMQ → Read DB)
│   └── cache/ (Redis Caching)
│
├── 📁 fraud-service/               # 🛡️ Fraud Detection (Port 8091)
│   ├── service/ (Rule Engine · Risk Score)
│   └── entity/ (FraudAlert · AmlAlert)
│
├── 📁 analytics-service/           # 📊 Analytics (Port 8093)
│   ├── service/ (Aggregation · Snapshot)
│   └── scheduler/ (Monthly Report)
│
├── 📁 audit-service/               # 🔍 Audit Trail (Port 8092)
│   └── consumer/ (AuditLogEvent → DB)
│
├── 🐳 docker-compose.yml           # One-Command Full Stack
├── 🏗️ pom.xml                     # Root Maven Multi-Module
└── 📖 README.md                    # You are here!
```

---

## 🚀 شروع سریع

### پیش‌نیازها

- ☕ Java 21 (JDK)
- 🔧 Maven 3.9+
- 🐳 Docker & Docker Compose
- 💡 IntelliJ IDEA (Ultimate یا Community)

### 1️⃣ باز کردن پروژه

```bash
# Extract ZIP
unzip banking-hybrid-architecture-complete.zip
cd banking-hybrid-architecture

# Open in IntelliJ
File → Open → banking-hybrid-architecture
# Maven → Reload Project
```

### 2️⃣ بیلد کل پروژه

```bash
mvn clean install -DskipTests
```

### 3️⃣ راه‌اندازی با Docker Compose (⭐ پیشنهادی)

```bash
# همه سرویس‌ها و زیرساخت‌ها با یک دستور
docker-compose up -d

# بررسی وضعیت
docker-compose ps

# مشاهده لاگ‌ها
docker-compose logs -f banking-monolith
```

> ✅ این دستور ۵ PostgreSQL، Redis، RabbitMQ و ۷ سرویس Spring Boot را همزمان بالا می‌آورد.

### 4️⃣ توقف همه سرویس‌ها

```bash
docker-compose down

# حذف دیتا (Volumeها)
docker-compose down -v
```

---

## 🔧 راه‌اندازی توسعه (بدون Docker)

### 1️⃣ زیرساخت‌ها (Docker فقط برای DB/Redis/RabbitMQ)

```bash
# PostgreSQL Monolith
docker run -d --name postgres-monolith \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_monolith_db -p 5432:5432 postgres:16-alpine

# PostgreSQL Transaction
docker run -d --name postgres-transaction \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_transaction_db -p 5433:5432 postgres:16-alpine

# PostgreSQL Fraud
docker run -d --name postgres-fraud \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_fraud_db -p 5434:5432 postgres:16-alpine

# PostgreSQL Analytics
docker run -d --name postgres-analytics \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_analytics_db -p 5435:5432 postgres:16-alpine

# PostgreSQL Audit
docker run -d --name postgres-audit \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=banking_audit_db -p 5436:5432 postgres:16-alpine

# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management-alpine
```

### 2️⃣ اجرای سرویس‌ها در IntelliJ

| ترتیب | سرویس | پورت | Run Config |
|-------|-------|------|------------|
| 1 | `shared-kernel` | — | `mvn install -pl shared-kernel` |
| 2 | `banking-monolith` | 8081 | `BankingMonolithApplication` |
| 3 | `transaction-write` | 8088 | `TransactionWriteApplication` |
| 4 | `transaction-read` | 8087 | `TransactionReadApplication` |
| 5 | `fraud-service` | 8091 | `FraudServiceApplication` |
| 6 | `analytics-service` | 8093 | `AnalyticsServiceApplication` |
| 7 | `audit-service` | 8092 | `AuditServiceApplication` |
| 8 | `api-gateway` | 8090 | `ApiGatewayApplication` |

> 💡 **نکته**: در IntelliJ روی هر `*Application.java` کلیک راست → **Run**.

---

## 🧪 تست API با Swagger

پس از راه‌اندازی، Swagger UI هر سرویس در دسترس است:

| سرویس | آدرس Swagger | توضیح |
|-------|-------------|-------|
| 🏛️ Monolith | http://localhost:8081/swagger-ui.html | User · Account · Card · Loan · Auth |
| ✍️ TX-Write | http://localhost:8088/swagger-ui.html | Create · Complete · Reverse Transaction |
| 📖 TX-Read | http://localhost:8087/swagger-ui.html | Query · Search · Filter Transactions |
| 🛡️ Fraud | http://localhost:8091/swagger-ui.html | Alerts · AML · Risk Score |
| 📊 Analytics | http://localhost:8093/swagger-ui.html | Spending · Reports · Snapshots |
| 🔍 Audit | http://localhost:8092/swagger-ui.html | Audit Logs · Trail |
| 🐰 RabbitMQ | http://localhost:15672 | guest / guest |

### 🔑 داده‌های Seed (آماده برای تست)

DataInitializer به‌صورت خودکار داده‌های اولیه را می‌سازد:

| کاربر | نام‌کاربری | رمز | نقش |
|-------|----------|-----|-----|
| 👨‍💼 Admin | `admin` | `admin123` | ADMIN |
| 👤 Customer | `customer` | `customer123` | CUSTOMER |

**حساب Seed**: `IR123456789012345678901234` (Balance: ۱۰۰,۰۰۰,۰۰۰)  
**کارت Seed**: `6104331234567890`  
**وام Seed**: ۱,۰۰۰,۰۰۰,۰۰۰ (PENDING)

### 🧪 جریان تست End-to-End

```bash
# 1. لاگین (گرفتن Token)
POST http://localhost:8081/api/auth/login
{"username":"admin","password":"admin123"}

# 2. لیست کاربران
GET http://localhost:8081/api/users
Authorization: Bearer <TOKEN>

# 3. واریز به حساب
POST http://localhost:8081/api/accounts/1/deposit?amount=50000000

# 4. ایجاد تراکنش
POST http://localhost:8088/api/transactions
{"fromAccountId":1,"toAccountId":1,"amount":25000000,"type":"TRANSFER","userId":1}

# 5. کامل کردن تراکنش
POST http://localhost:8088/api/transactions/1/complete

# 6. مشاهده در Read Model (CQRS)
GET http://localhost:8087/api/transactions

# 7. مشاهده Fraud Alert
GET http://localhost:8091/api/fraud

# 8. مشاهده Analytics
GET http://localhost:8093/api/analytics

# 9. مشاهده Audit Logs
GET http://localhost:8092/api/audit
```

---

## 🔐 امنیت

- ✅ **JWT Authentication** (Access + Refresh Token)
- ✅ **Role-Based Access Control** (ADMIN, CUSTOMER, SUPER_ADMIN)
- ✅ **Method-Level Security** (`@PreAuthorize` with SPEL)
- ✅ **CORS** (Cross-Origin Resource Sharing)
- ✅ **CSP** (Content Security Policy)
- ✅ **HSTS** (HTTP Strict Transport Security)
- ✅ **XSS Protection** (Headers + Input Validation)
- ✅ **Rate Limiting** (Redis-based در Gateway)
- ✅ **Internal API Protection** (IP-based `/internal/**`)
- ✅ **Soft Delete** (هیچ داده‌ای فیزیکی حذف نمی‌شود)
- ✅ **Optimistic Locking** (`@Version` برای جلوگیری از Race Condition)

---

## 📊 CQRS و Event-Driven

### CQRS Pattern

```
┌─────────────────┐         ┌─────────────────┐
│  TX-Write (8088)│         │  TX-Read (8087)  │
│   Write Model    │  ────►  │   Read Model     │
│   PostgreSQL     │  Event  │   PostgreSQL     │
│   (Source)      │  Bus    │   (Replica)      │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │  ┌───────────────────┐    │
         └──►  🐰 RabbitMQ      │◄───┘
             │  transaction.*   │
             └───────────────────┘
```

### Event Flow

| Event | Publisher | Consumers | Action |
|-------|-----------|-----------|--------|
| `TransactionCompletedEvent` | transaction-write | tx-read, fraud, analytics, audit, notification | Sync + Analyze + Notify |
| `LoanApprovedEvent` | banking-monolith | notification | Push Notification + SSE |
| `AccountCreatedEvent` | banking-monolith | — | Audit + Log |
| `FraudDetectedEvent` | fraud-service | notification | Fraud Alert Notification |

---

## 🐳 Docker Compose

```yaml
version: '3.8'
services:
  # 5 PostgreSQL databases
  postgres-monolith:  ...  (port 5432)
  postgres-transaction: ... (port 5433)
  postgres-fraud:     ...  (port 5434)
  postgres-analytics: ...  (port 5435)
  postgres-audit:     ...  (port 5436)
  
  # Cache & Message Broker
  redis:      port 6379
  rabbitmq:   ports 5672, 15672
  
  # 7 Spring Boot Services
  banking-monolith:     port 8081
  api-gateway:          port 8090
  transaction-read:     port 8087
  transaction-write:    port 8088
  fraud-service:        port 8091
  analytics-service:    port 8093
  audit-service:        port 8092
```

> 🚀 **یک دستور**: `docker-compose up -d` = کل سیستم بانکداری!

---

## 📝 لاگ‌گیری و مشاهده‌پذیری

هر سرویس دارای **Log Markers** منحصربه‌فرد برای trace سریع:

```
[TX-CREATE]   ✅ Transaction created: id=123, trackingCode=TXN...
[TX-BALANCE]  ✅ Balance check passed: account=1, balance=100000000
[TX-REST]     ✅ Monolith deposit OK for accountId=1
[FRAUD-ANALYZE] ✅ Risk score=75, Level=CHALLENGE, Rules=[LARGE_TRANSACTION]
[LOAN-APPROVE] ✅ Loan approved: id=1, monthlyInstallment=90258338
[NOTIF-RECV]  ✅ Received TransactionCompletedEvent: txId=123
[NOTIF-SSE]   ✅ SSE broadcast sent: type=TRANSACTION_DONE
```

---

## 🎨 ویژگی‌های برجسته

| ویژگی | توضیح | فایده |
|-------|-------|-------|
| **🔀 Hybrid Architecture** | Monolith + Microservices | توسعه سریع + مقیاس‌پذیری |
| **📊 CQRS** | Read/Write Separation | Performance · Scalability |
| **🐰 Event-Driven** | RabbitMQ Fan-out | Loose Coupling · Resilience |
| **💾 Soft Delete** | Logical Deletion | Audit · Compliance · Recovery |
| **🔒 Multi-Layer Security** | JWT + RBAC + CSP + HSTS | Enterprise-Grade Protection |
| **⚡ Virtual Threads** | Java 21 Virtual Threads | Millions of Concurrent Connections |
| **🗺️ MapStruct** | Compile-Time Mapping | Zero Reflection · High Performance |
| **📈 Business Logic** | PMT Formula · Risk Engine | Real-World Banking Rules |
| **🐳 Docker Ready** | One-Command Deploy | DevOps · CI/CD Ready |
| **📖 Swagger UI** | Interactive API Docs | Easy Testing · Integration |

---

## 🤝 مشارکت

از مشارکت شما استقبال می‌کنیم! 🎉

1. Fork کنید
2. Branch بسازید (`git checkout -b feature/amazing-feature`)
3. Commit کنید (`git commit -m 'Add amazing feature'`)
4. Push کنید (`git push origin feature/amazing-feature`)
5. Pull Request بسازید

---

## 📜 مجوز

Distributed under the MIT License. See `LICENSE` for more information.

---

<div align="center">

**🌟 اگر این پروژه به شما کمک کرد، ستاره بدید!** 🌟

<p align="center">
  <img src="https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/rainbow.png" alt="divider" width="100%">
</p>

Made with ❤️ and ☕ by **SpringBank Team**

</div>
