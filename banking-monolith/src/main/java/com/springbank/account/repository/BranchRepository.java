package com.springbank.account.repository;

import com.springbank.account.entity.Branch;
import com.springbank.common.repository.BaseEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends BaseEntityRepository<Branch, Long> {
    Optional<Branch> findByCode(String code);
    boolean existsByCode(String code);
}
