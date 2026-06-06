package com.springbank.transaction.write;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Transaction Write Service (CQRS Write Model)
 * Port: 8088 | DB: 5433 | Connects to Monolith (8081) for balance checks
 * Flow: Create TX → Check Balance → Publish Event → Complete TX → Update Balances
 */
@Slf4j
@SpringBootApplication
public class TransactionWriteApplication {

    public static void main(String[] args) {
        log.info("🚀 [STEP 3/7] Starting TRANSACTION-WRITE on port 8088...");
        log.info("🔗 Depends on: RabbitMQ (5672), PostgreSQL (5433), Monolith (8081)");
        SpringApplication.run(TransactionWriteApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("✅ [STEP 3/7] ✅ TRANSACTION-WRITE is UP on port 8088");
        log.info("📋 Swagger UI: http://localhost:8088/swagger-ui.html");
        log.info("⚠️  If 'Cannot verify account balance' error appears → Monolith (8081) is not running!");
        log.info("⚠️  If events not reaching other services → Check RabbitMQ (5672) is running!");
    }
}
