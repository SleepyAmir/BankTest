package com.springbank.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class EncryptionAspect {

    @Around("com.springbank.aspect.BasePointcuts.encryptedServices()")
    public Object encryptAround(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("Encryption aspect: processing sensitive data");
        return joinPoint.proceed();
    }
}
