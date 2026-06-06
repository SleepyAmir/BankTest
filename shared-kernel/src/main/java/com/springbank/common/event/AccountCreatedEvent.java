package com.springbank.common.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountCreatedEvent {
    private Long accountId;
    private Long userId;
    private String accountNumber;
    private BigDecimal initialBalance;
    private LocalDateTime createdAt;
}
