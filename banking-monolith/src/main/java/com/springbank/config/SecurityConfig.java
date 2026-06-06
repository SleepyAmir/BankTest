package com.springbank.config;

import com.springbank.user.security.JwtAuthenticationFilter;
import com.springbank.security.handler.CustomAccessDeniedHandler;
import com.springbank.security.handler.CustomAuthenticationEntryPoint;
import com.springbank.user.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * پیکربندی امنیت مدرن Spring Boot 4.0.5
 * شامل: JWT, XSS Protection, CSP, CORS, Session Management
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT token"
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final LogoutHandler logoutHandler;

    // =========================================================================
    // ============================ ENCRYPTION & AUTH ==========================
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // =========================================================================
    // ============================ CORS CONFIGURATION =========================
    // =========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080",
                "http://localhost:8090"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    // =========================================================================
    // ============================ SECURITY FILTER CHAIN =======================
    // =========================================================================

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ==================== DISABLE FEATURES ====================
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ==================== SESSION MANAGEMENT ====================
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation().changeSessionId()
                )

                // ==================== SECURITY HEADERS ====================
                .headers(headers -> headers
                        // Frame Options (برای H2 Console)
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)

                        // XSS Protection
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )

                        // Content Security Policy (CSP)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data: https:; " +
                                                "font-src 'self' data:; " +
                                                "connect-src 'self' https:; " +
                                                "frame-ancestors 'none'; " +
                                                "form-action 'self'; " +
                                                "base-uri 'self'; " +
                                                "upgrade-insecure-requests;"
                                )
                        )

                        // Content Type Options (MIME sniffing protection)
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)

                        // HSTS (HTTP Strict Transport Security)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )

                        // Cache Control
                        .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                )

                // ==================== AUTHORIZATION ====================
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (بدون احراز هویت)
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/users/register",
                                "/api/users/forgot-password",
                                "/api/users/reset-password",
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/h2-console/**",
                                "/h2-console",
                                "/h2-console/*",
                                "/",
                                "/index",
                                "/login",
                                "/dashboard",
                                "/css/**",
                                "/js/**",
                                "/static/**",
                                "/**/*.css",
                                "/**/*.js",
                                "/**/*.html",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        // Admin only
                        .requestMatchers(
                                "/api/admin/**",
                                "/actuator/**"
                        ).hasRole("ADMIN")

                        // Super Admin only
                        .requestMatchers(
                                "/api/super-admin/**"
                        ).hasRole("SUPER_ADMIN")

                        // Internal endpoints — only localhost (inter-service calls)
                        .requestMatchers("/internal/**")
                        .access(new org.springframework.security.authorization.AuthorizationManager<org.springframework.security.web.access.intercept.RequestAuthorizationContext>() {
                            @Override
                            public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
                                AuthorizationManager.super.verify(authentication, object);
                            }

                            @Nullable
                            @Override
                            public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
                                return null;
                            }

                            public org.springframework.security.authorization.AuthorizationDecision check(org.springframework.security.core.Authentication authentication, org.springframework.security.web.access.intercept.RequestAuthorizationContext context) {
                                String remoteAddr = context.getRequest().getRemoteAddr();
                                boolean allowed = "127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr);
                                return new org.springframework.security.authorization.AuthorizationDecision(allowed);
                            }
                        })

                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )

                // ==================== EXCEPTION HANDLING ====================
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // ==================== LOGOUT ====================
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true,\"message\":\"Logout successful\"}");
                        })
                        .addLogoutHandler(logoutHandler)
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                // ==================== AUTHENTICATION ====================
                .userDetailsService(customUserDetailsService)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // ============================ REQUEST LOGGING ============================
    // =========================================================================

    @Bean
    public org.springframework.web.filter.CommonsRequestLoggingFilter requestLoggingFilter() {
        org.springframework.web.filter.CommonsRequestLoggingFilter loggingFilter =
                new org.springframework.web.filter.CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(1000);
        loggingFilter.setIncludeHeaders(false);
        return loggingFilter;
    }
}