package com.springbank.common.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanApprovedEvent {
    private Long loanId;
    private Long userId;
    private Long accountId;
    private BigDecimal amount;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
