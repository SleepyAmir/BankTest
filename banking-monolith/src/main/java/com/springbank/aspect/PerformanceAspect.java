package com.springbank.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    private static final long WARN_THRESHOLD_MS = 500;

    @Around("com.springbank.aspect.BasePointcuts.allServiceMethods()")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        String methodName = joinPoint.getSignature().toShortString();
        if (duration > WARN_THRESHOLD_MS) {
            log.warn("Performance warning: {} took {}ms", methodName, duration);
        } else {
            log.debug("Performance: {} took {}ms", methodName, duration);
        }
        return result;
    }
}
