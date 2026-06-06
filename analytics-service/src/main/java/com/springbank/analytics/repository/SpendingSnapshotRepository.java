package com.springbank.analytics.repository;

import com.springbank.analytics.entity.SpendingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpendingSnapshotRepository extends JpaRepository<SpendingSnapshot, Long> {
    List<SpendingSnapshot> findByUserId(Long userId);
    Optional<SpendingSnapshot> findByUserIdAndSnapshotMonth(Long userId, YearMonth snapshotMonth);
    List<SpendingSnapshot> findByUserIdOrderBySnapshotMonthDesc(Long userId);
}
