package com.springbank.user.repository;

import com.springbank.common.enums.KycStatus;
import com.springbank.common.repository.BaseEntityRepository;
import com.springbank.user.entity.KycVerification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycVerificationRepository extends BaseEntityRepository<KycVerification, Long> {

    Optional<KycVerification> findByUserId(Long userId);

    List<KycVerification> findByStatus(KycStatus status);

    boolean existsByUserId(Long userId);
}
