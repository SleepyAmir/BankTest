package com.springbank.common.exception;

import com.springbank.common.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER — Centralized Error Handling
 * ============================================================================
 * This class catches ALL exceptions thrown by ANY controller and converts them
 * to a standard ApiResponse format. Check logs for the exact cause.
 *
 * LOG MARKERS: [ERR-404] [ERR-400] [ERR-409] [ERR-500]
 * ============================================================================
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.error("[ERR-404] Resource not found: {} | Path: {}", ex.getMessage(), request.getDescription(false));
        log.error("[ERR-404] 💡 TIP: Check if the ID exists in the database. Use GET endpoints to verify.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value(), request.getDescription(false)));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.error("[ERR-400] Business error: {} | Path: {}", ex.getMessage(), request.getDescription(false));
        log.error("[ERR-400] 💡 TIP: Check request parameters/ body. E.g., duplicate account number, invalid status.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), request.getDescription(false)));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        log.error("[ERR-404] Entity not found: {} | Path: {}", ex.getMessage(), request.getDescription(false));
        log.error("[ERR-404] 💡 TIP: JPA entity lookup failed. The record may have been soft-deleted.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value(), request.getDescription(false)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.error("[ERR-400] Illegal argument: {} | Path: {}", ex.getMessage(), request.getDescription(false));
        log.error("[ERR-400] 💡 TIP: Validation failed. Check required fields, enum values, number formats.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), request.getDescription(false)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.error("[ERR-409] Illegal state: {} | Path: {}", ex.getMessage(), request.getDescription(false));
        log.error("[ERR-409] 💡 TIP: Business state conflict. E.g., trying to complete a non-PENDING transaction.");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value(), request.getDescription(false)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, WebRequest request) {
        log.error("[ERR-500] UNEXPECTED ERROR: {} | Path: {}", ex.getMessage(), request.getDescription(false));
        log.error("[ERR-500] 💡 TIP: Check if PostgreSQL, Redis, RabbitMQ are running.");
        log.error("[ERR-500] 💡 TIP: Check service startup order: Monolith → TX-Write → TX-Read → Fraud → Analytics → Audit → Gateway");
        log.error("[ERR-500] Stack trace:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Check server logs for details.",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getDescription(false)));
    }
}
