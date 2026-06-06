package com.springbank.aspect;

import com.springbank.common.annotation.Auditable;
import com.springbank.common.event.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    public static final String EXCHANGE = "banking.exchange";
    public static final String ROUTING_KEY = "audit.log";

    private final RabbitTemplate rabbitTemplate;

    @AfterReturning(
        pointcut = "com.springbank.aspect.BasePointcuts.writeServiceMethods() && @annotation(auditable)",
        returning = "result"
    )
    public void audit(JoinPoint jp, Auditable auditable, Object result) {
        String actor = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "system";

        Long entityId = extractId(result);

        var event = AuditLogEvent.builder()
                .actorUsername(actor)
                .action(auditable.action())
                .entityType(auditable.entity())
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        log.debug("Audit event published: {} on {}", auditable.action(), auditable.entity());
    }

    private Long extractId(Object result) {
        if (result == null) return null;
        try {
            return (Long) result.getClass().getMethod("getId").invoke(result);
        } catch (Exception e) {
            return null;
        }
    }
}
