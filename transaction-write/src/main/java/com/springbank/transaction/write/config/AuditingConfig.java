package com.springbank.transaction.write.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * ============================================================================
 * JPA AUDITING CONFIG
 * ============================================================================
 * فعال‌سازی JPA Auditing تا فیلدهای @CreatedDate / @LastModifiedDate (مثل created_at,
 * updated_at در BaseEntity) به‌صورت خودکار هنگام ذخیره پر شوند.
 *
 * بدون این، ستون created_at با NULL ذخیره می‌شود و خطای
 * "null value in column created_at violates not-null constraint" رخ می‌دهد.
 * ============================================================================
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("transaction-write");
    }
}
