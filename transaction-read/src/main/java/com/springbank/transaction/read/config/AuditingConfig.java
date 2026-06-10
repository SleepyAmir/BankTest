package com.springbank.transaction.read.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * فعال‌سازی JPA Auditing تا created_at/updated_at (از BaseEntity) خودکار پر شوند.
 * بدون این، ذخیره‌ی تراکنش در read-model با خطای not-null روی created_at شکست می‌خورد.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("transaction-read");
    }
}
