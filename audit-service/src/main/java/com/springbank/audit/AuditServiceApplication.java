package com.springbank.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Audit Log Service
 * Port: 8092 | DB: 5436 | RabbitMQ Consumer: audit.queue (audit.*)
 * Flow: Receives AuditLogEvent → Persists to DB → Serves Query API
 */
@Slf4j

@SpringBootApplication
@EnableScheduling
public class AuditServiceApplication {

    public static void main(String[] args) {
        log.info("🚀 [STEP 7/7] Starting AUDIT-SERVICE on port 8092...");
        log.info("🔗 Depends on: RabbitMQ (5672), PostgreSQL (5436)");
        SpringApplication.run(AuditServiceApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("✅ [STEP 7/7] ✅ AUDIT-SERVICE is UP on port 8092");
        log.info("📋 Swagger UI: http://localhost:8092/swagger-ui.html");
        log.info("⚠️  NOTE: Audit events are sent by @Auditable aspect in Monolith. If empty, check AOP is working.");
    }
}
