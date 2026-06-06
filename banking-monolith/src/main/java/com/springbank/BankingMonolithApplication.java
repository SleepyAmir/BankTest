package com.springbank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * ██████╗  █████╗ ███╗   ██╗██╗  ██╗██╗███╗   ██╗ ██████╗
 * ██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝██║████╗  ██║██╔════╝
 * ██████╔╝███████║██╔██╗ ██║█████╔╝ ██║██╔██╗ ██║██║  ███╗
 * ██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ ██║██║╚██╗██║██║   ██║
 * ██████╔╝██║  ██║██║ ╚████║██║  ██╗██║██║ ╚████║╚██████╔╝
 * ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝
 *
 * Banking Monolith Core Service
 * Ports: 8081 (HTTP) | DB: 5432 | Redis: 6379 | RabbitMQ: 5672
 * Modules: User, Account, Card, Loan, Notification, Security, AOP
 */
@Slf4j
@SpringBootApplication
public class BankingMonolithApplication {

    public static void main(String[] args) {
        log.info("🚀 [STEP 1/7] Starting BANKING-MONOLITH on port 8081...");
        SpringApplication.run(BankingMonolithApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("✅ [STEP 1/7] ✅ BANKING-MONOLITH is UP AND RUNNING on port 8081");
        log.info("📋 Available endpoints: /api/auth/**, /api/users/**, /api/accounts/**, /api/cards/**, /api/loans/**, /api/notifications/**");
        log.info("📋 Swagger UI: http://localhost:8081/swagger-ui.html");
        log.info("📋 Health Check: http://localhost:8081/actuator/health (if enabled)");
        log.info("⚠️  NOTE: Make sure PostgreSQL (5432), Redis (6379), RabbitMQ (5672) are running!");
    }
}
