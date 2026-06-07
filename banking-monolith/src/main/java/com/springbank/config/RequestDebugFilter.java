package com.springbank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("[DEBUG-REQUEST] >>> {} {} | Origin={} | Content-Type={} | Auth={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader("Origin"),
                request.getContentType(),
                request.getHeader("Authorization") != null ? "Bearer ***" : "none");

        filterChain.doFilter(request, response);

        log.info("[DEBUG-REQUEST] <<< {} {} | Status={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus());
    }
}