package com.springbank.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ValidationAspect {

    @Before("com.springbank.aspect.BasePointcuts.writeServiceMethods()")
    public void validateBeforeWrite() {
        log.debug("Validation aspect triggered before write operation");
    }
}
