package com.springbank.config;

import com.springbank.user.security.JwtAuthenticationFilter;
import com.springbank.security.handler.CustomAccessDeniedHandler;
import com.springbank.security.handler.CustomAuthenticationEntryPoint;
import com.springbank.user.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * SECURITY CONFIG — نسخه نهایی بدون CSRF / FormLogin / HttpBasic
 * ============================================================================
 * اگر این فایل لود شد، باید در کنسول لاگ زیر چاپ شود:
 * [SEC-CONFIG] ======== SecurityConfig LOADED ========
 * ============================================================================
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * آیا آدرس متعلق به loopback یا شبکه‌ی خصوصی داخلی است؟
     * شامل: 127.0.0.1، ::1، و رنج‌های خصوصی (10.x، 172.16-31.x، 192.168.x) برای کارکرد در Docker.
     */
    private static boolean isInternalAddress(String addr) {
        if (addr == null) return false;
        if ("127.0.0.1".equals(addr) || "::1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr)) {
            return true;
        }
        if (addr.startsWith("10.") || addr.startsWith("192.168.")) {
            return true;
        }
        // 172.16.0.0 – 172.31.255.255 (رنج خصوصی پیش‌فرض Docker)
        if (addr.startsWith("172.")) {
            String[] parts = addr.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // هر پورت localhost / 127.0.0.1 مجاز است (با allowCredentials باید از Patterns استفاده شود نه "*")
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("[SEC-CONFIG] ======== SecurityConfig LOADED ========");
        log.info("[SEC-CONFIG] CSRF=DISABLED | FormLogin=DISABLED | HttpBasic=DISABLED | Stateless JWT");

        http
                // -------- غیرفعال کردن کامل CSRF / FormLogin / HttpBasic --------
                .csrf(csrf -> {
                    log.info("[SEC-CONFIG] CSRF disabled explicitly");
                    csrf.disable();
                })
                .formLogin(formLogin -> {
                    log.info("[SEC-CONFIG] FormLogin disabled explicitly");
                    formLogin.disable();
                })
                .httpBasic(httpBasic -> {
                    log.info("[SEC-CONFIG] HttpBasic disabled explicitly");
                    httpBasic.disable();
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // -------- مجوزها --------
                .authorizeHttpRequests(auth -> auth
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
                                "/css/**", "/js/**", "/static/**",
                                "/error", "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/api/admin/**", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/internal/**")
                        .access((authentication, context) -> {
                            // endpointهای داخلی سرویس‌به‌سرویس: loopback + شبکه‌ی خصوصی داخلی (Docker).
                            // این مسیرها به بیرون expose نمی‌شوند؛ فقط از داخل شبکه‌ی سرویس‌ها قابل دسترسی‌اند.
                            String remoteAddr = context.getRequest().getRemoteAddr();
                            boolean allowed = isInternalAddress(remoteAddr);
                            return new org.springframework.security.authorization.AuthorizationDecision(allowed);
                        })
                        .anyRequest().authenticated()
                )

                // -------- هندل خطا --------
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // -------- logout --------
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

                // -------- JWT filter --------
                .userDetailsService(customUserDetailsService)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}