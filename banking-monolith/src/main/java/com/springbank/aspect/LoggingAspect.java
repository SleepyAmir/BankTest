package com.springbank.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("com.springbank.aspect.BasePointcuts.allServiceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        log.debug(">> {} - args: {}", methodName, joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            log.debug("<< {} - result: {}", methodName, result);
            return result;
        } catch (Throwable e) {
            log.error("!! {} - exception: {}", methodName, e.getMessage());
            throw e;
        }
    }
}
