# ✅ وضعیت فعلی پروژه — چه چیزهایی انجام شده و چه چیزهایی مونده

## ✅ انجام شده (Done)

### ساختار و تنظیمات
- [x] Root POM + 8 ماژول Maven (multi-module)
- [x] POM هر ماژول با dependency های درست
- [x] Application class برای هر 7 سرویس
- [x] application.yml برای هر سرویس (پورت، دیتابیس، RabbitMQ)
- [x] shared-kernel با BaseEntity + 21 Enum
- [x] Entity های همه ماژول‌ها با relation درست (ID برای cross-service)

### لایه امنیت (Security)
- [x] SecurityConfig (JWT, CORS, CSP, HSTS, XSS, Session)
- [x] JwtTokenProvider (generate/validate token)
- [x] JwtAuthenticationFilter
- [x] CustomUserDetailsService + SecurityUser adapter
- [x] CustomAccessDeniedHandler + CustomAuthenticationEntryPoint + CustomLogoutHandler
- [x] SecurityUserService

### لایه دیتا (User Module)
- [x] BaseEntityRepository (generic, soft-delete, optimistic locking)
- [x] UserRepository, RoleRepository, PermissionRepository
- [x] BaseEntityService (abstract generic CRUD)
- [x] UserService, RoleService, PermissionService, ProfileService
- [x] DTO skeleton های User/Role/Permission (record)
- [x] Mapper skeleton های User/Role/Permission/Profile (MapStruct)

---

## ❌ باقی‌مانده (Remaining Tasks)

### 🔴 بحرانی — بدون این‌ها پروژه کامپایل/اجرا نمی‌شه

| # | Task | تعداد | توضیح |
|---|------|-------|-------|
| 1 | **Controllers** | ~15-20 | AuthController, UserController, RoleController, PermissionController, AccountController, CardController, LoanController, NotificationController (SSE), InternalController, TransactionReadController, TransactionWriteController, FraudController, AnalyticsController, AuditController |
| 2 | **Repositories باقی‌مانده** | ~12 | AccountRepository, CardRepository, LoanRepository, LoanInstallmentRepository, CreditScoreRepository, BranchRepository, ExchangeRateRepository, NotificationRepository, AuditLogRepository, TransactionRepository (2x), FraudAlertRepository, AmlAlertRepository, SpendingSnapshotRepository |
| 3 | **Services باقی‌مانده** | ~15 | AccountReadService, AccountWriteService, CardReadService, CardWriteService, LoanReadService, LoanWriteService, CreditScoreService, NotificationService, AuditLogService, TransactionReadService, TransactionWriteService, FraudAnalysisService, AnalyticsService, AuthService |
| 4 | **DTO های باقی‌مانده** | ~20+ | AccountCreateDto/ResponseDto, CardCreateDto/ResponseDto, LoanCreateDto/ResponseDto, TransactionCreateDto/ResponseDto, NotificationDto, AuditLogDto, FraudAlertDto, etc. |
| 5 | **Mapper های باقی‌مانده** | ~10 | AccountMapper, CardMapper, LoanMapper, TransactionMapper, FraudAlertMapper, AuditLogMapper, NotificationMapper, SpendingSnapshotMapper |

### 🟡 خیلی مهم — برای معماری Hybrid ضروری‌ست

| # | Task | توضیح |
|---|------|-------|
| 6 | **RabbitMQ Config + Event classes** | Exchange, Queue, RoutingKey binding + Event POJOs (TransactionCompletedEvent, FraudDetectedEvent, LoanApprovedEvent, AccountCreatedEvent, AuditLogEvent) |
| 7 | **RabbitMQ Publishers** | TransactionEventPublisher (transaction-write), AuditEventPublisher (monolith) |
| 8 | **RabbitMQ Consumers** | FraudEventConsumer (fraud-service), AnalyticsEventConsumer (analytics-service), AuditEventConsumer (audit-service), NotificationEventConsumer (monolith) |
| 9 | **AOP Aspects** | AuditAspect (@Auditable), EncryptionAspect (AES-256-GCM), LoggingAspect, PerformanceAspect, ValidationAspect, BasePointcuts |
| 10 | **InternalController** | REST endpoints فقط برای سرویس‌های داخلی: /internal/accounts/{id}/balance, /internal/users/{id} |
| 11 | **Exception Handling** | BusinessException, ResourceNotFoundException, GlobalExceptionHandler (@ControllerAdvice) |
| 12 | **Encryption Utils** | AES-256-GCM encryption/decryption برای cardNumber, cvv2, pin |
| 13 | **API Gateway Filters** | JwtAuthFilter, RateLimitFilter (Redis), RouteFilter, LoggingFilter |

### 🟢 متوسط — برای production نیازه

| # | Task | توضیح |
|---|------|-------|
| 14 | **Swagger/OpenAPI Config** | SwaggerConfig برای هر سرویس + aggregation در Gateway |
| 15 | **Redis Config** | RedisCacheManager, @Cacheable/@CacheEvict در ReadService ها |
| 16 | **AuditingConfig** | @EnableJpaAuditing برای createdAt/updatedAt/createdBy/updatedBy |
| 17 | **Scheduler Jobs** | InstallmentReminderJob (nightly), CreditScoreRecalculationJob |
| 18 | **SSE Controller** | NotificationSseController برای real-time push |
| 19 | **Docker Compose** | PostgreSQL ×5, Redis, RabbitMQ یکجا |
| 20 | **Database Schema (DDL)** | SQL scripts برای همه 5 database — یا `ddl-auto=create` برای dev |

### 🟣 اختیاری — polish

| # | Task | توضیح |
|---|------|-------|
| 21 | **Unit/Integration Tests** | @SpringBootTest, Testcontainers |
| 22 | **Virtual Threads (Java 21)** | `spring.threads.virtual.enabled=true` |
| 23 | **Resilience4j Circuit Breaker** | برای REST calls بین سرویس‌ها |
| 24 | **Flyway/Liquibase Migration** | DB migration به جای Hibernate DDL |
| 25 | **Dockerfile برای هر سرویس** | Multi-stage build |

---

## 🎯 پیشنهاد اولویت‌بندی (اگه می‌خوای قدم به قدم جلو بری)

### فاز ۱: Monolith Core (هفته ۱)
1. GlobalExceptionHandler + BusinessException + ResourceNotFoundException
2. AuthController (POST /api/auth/login, /api/auth/register, /api/auth/refresh)
3. UserController, RoleController, PermissionController
4. AccountReadService + AccountWriteService + AccountRepository + AccountController
5. CardReadService + CardWriteService + CardRepository + CardController (با EncryptionAspect)
6. LoanReadService + LoanWriteService + LoanRepository + CreditScoreRepository + LoanController
7. AuditingConfig + AuditAspect + @Auditable annotation

### فاز ۲: Communication + Supporting (هفته ۲)
8. RabbitMQConfig + Event classes (shared-kernel)
9. InternalController (monolith)
10. NotificationService + NotificationEventConsumer + SSE Controller
11. AuditLogService + AuditEventConsumer (audit-service)
12. API Gateway Filters + Routing

### فاز ۳: Microservices (هفته ۳)
13. TransactionWriteService + Repository + Controller + EventPublisher
14. TransactionReadService + Repository + Controller (WebFlux یا JPA)
15. FraudService + FraudEventConsumer + Repository + Controller
16. AnalyticsService + AnalyticsEventConsumer + Repository + Controller + Scheduler

### فاز ۴: Infrastructure + Polish (هفته ۴)
17. Docker Compose (PostgreSQL ×5, Redis, RabbitMQ)
18. Swagger aggregation
19. Integration Tests
20. Dockerfiles

---

**خلاصه:** حدود **70-80 کلاس** هنوز باید نوشته بشه تا پروژه "کامل" بشه و قابل اجرا.
