package com.springbank.account.repository;

import com.springbank.account.entity.OpenBankingConsent;
import com.springbank.common.repository.BaseEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpenBankingConsentRepository extends BaseEntityRepository<OpenBankingConsent, Long> {
    List<OpenBankingConsent> findByUserIdAndActiveTrue(Long userId);
}
