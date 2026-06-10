package com.springbank.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * ============================================================================
 * GATEWAY SECURITY CONFIG
 * ============================================================================
 * Gateway route and authenticate JWT.
 * CSRF / FormLogin / HttpBasic are DISABLED.
 * ============================================================================
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        log.info("[GATEWAY-SEC] ======== Gateway SecurityConfig LOADED ========");
        log.info("[GATEWAY-SEC] CSRF=DISABLED | FormLogin=DISABLED | HttpBasic=DISABLED | CORS=ENABLED");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(cors -> {
                    log.info("[GATEWAY-SEC] CORS enabled via application.yml globalcors");
                })
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll() // the JwtAuthFilter handles the actual path validation
                )
                .build();
    }
}
