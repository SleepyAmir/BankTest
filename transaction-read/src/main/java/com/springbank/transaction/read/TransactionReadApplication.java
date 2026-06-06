package com.springbank.transaction.read;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TransactionReadApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionReadApplication.class, args);
    }
}
