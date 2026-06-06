package com.springbank.aspect;

import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
public class BasePointcuts {

    @Pointcut("execution(* com.springbank.*.service.*WriteService.*(..))")
    public void writeServiceMethods() {}

    @Pointcut("execution(* com.springbank.card.service..*(..)) || " +
              "execution(* com.springbank.user.service..*(..))")
    public void encryptedServices() {}

    @Pointcut("execution(* com.springbank.*.service..*(..))")
    public void allServiceMethods() {}
}
