package com.springbank.account.repository;

import com.springbank.account.entity.ExchangeRate;
import com.springbank.common.repository.BaseEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends BaseEntityRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByCurrencyCodeAndEffectiveDate(String currencyCode, java.time.LocalDate effectiveDate);
}
