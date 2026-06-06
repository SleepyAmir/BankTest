package com.springbank.account.entity;
import com.springbank.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ExchangeRate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // کد ارز: USD, EUR, AED, GBP, ...
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    // نرخ برابری نسبت به ریال
    @Column(name = "rate_to_irr", nullable = false, precision = 19, scale = 4)
    private BigDecimal rateToIrr;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    // منبع: CENTRAL_BANK, FREE_MARKET
    @Column(length = 30)
    private String source;

    // ======== HELPER METHODS ========

    public BigDecimal convertToIrr(BigDecimal amount) {
        return amount.multiply(rateToIrr);
    }

    public BigDecimal convertFromIrr(BigDecimal irrAmount) {
        return irrAmount.divide(rateToIrr, 4, java.math.RoundingMode.HALF_UP);
    }
}
