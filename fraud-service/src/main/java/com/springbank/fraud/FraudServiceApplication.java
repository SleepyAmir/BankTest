package com.springbank.fraud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Fraud Detection Service
 * Port: 8091 | DB: 5434 | RabbitMQ Consumer: fraud.queue (transaction.*)
 * Flow: Receives TX Event → Scores Risk → Saves FraudAlert/AMLAlert
 */
@Slf4j
@SpringBootApplication
public class FraudServiceApplication {

    public static void main(String[] args) {
        log.info("🚀 [STEP 5/7] Starting FRAUD-SERVICE on port 8091...");
        log.info("🔗 Depends on: RabbitMQ (5672), PostgreSQL (5434)");
        SpringApplication.run(FraudServiceApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("✅ [STEP 5/7] ✅ FRAUD-SERVICE is UP on port 8091");
        log.info("📋 Swagger UI: http://localhost:8091/swagger-ui.html");
        log.info("⚠️  If no fraud alerts generated → Check if transaction amount > 50M or device fingerprint is missing!");
    }
}
