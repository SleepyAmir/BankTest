package com.springbank.transaction.read;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Transaction Read Service (CQRS Read Model)
 * Port: 8087 | DB: 5433 | Redis: 6379 | RabbitMQ Consumer: transaction.read.queue
 * Flow: Receives RabbitMQ Event → Saves to Read DB → Serves Queries via Cache
 */
@Slf4j
@SpringBootApplication
@EnableCaching
public class TransactionReadApplication {

    public static void main(String[] args) {
        log.info("🚀 [STEP 4/7] Starting TRANSACTION-READ on port 8087...");
        log.info("🔗 Depends on: RabbitMQ (5672), PostgreSQL (5433), Redis (6379)");
        SpringApplication.run(TransactionReadApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("✅ [STEP 4/7] ✅ TRANSACTION-READ is UP on port 8087");
        log.info("📋 Swagger UI: http://localhost:8087/swagger-ui.html");
        log.info("⚠️  If no transactions appear in queries → Check RabbitMQ queue 'transaction.read.queue' has messages!");
        log.info("⚠️  If Redis connection fails → Cache falls back to DB (slower but works)");
    }
}
