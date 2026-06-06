package com.springbank.common.repository;

import com.springbank.common.entity.BaseEntity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@NoRepositoryBean
public interface BaseEntityRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

    @Query("select e from #{#entityName} e where e.id = :id and e.deleted = false")
    Optional<T> findActiveById(@Param("id") ID id);

    @Query("select e from #{#entityName} e where e.id = :id")
    Optional<T> findByIdIncludingDeleted(@Param("id") ID id);

    @Query("select e from #{#entityName} e where e.deleted = false")
    List<T> findAllActive();

    @Modifying
    @Transactional
    @Query("update #{#entityName} e set e.deleted = true, e.deletedAt = :now, " +
            "e.version = e.version + 1 where e.id = :id and e.version = :currentVersion")
    int softDeleteByIdWithVersion(@Param("id") ID id,
                                  @Param("now") LocalDateTime now,
                                  @Param("currentVersion") Long currentVersion);


    default T updateWithConflictMessage(ID id, Consumer<T> updateLogic) {
        try {
            T entity = findActiveById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));

            updateLogic.accept(entity);
            return save(entity);

        } catch (OptimisticLockException e) {
            throw new java.util.ConcurrentModificationException(
                    "This record is being modified by another user. Please refresh and try again."
            );
        }
    }

    default boolean softDelete(ID id) {
        return softDeleteWithRetry(id, 3);
    }

    default boolean softDeleteWithRetry(ID id, int maxRetries) {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                T entity = findByIdIncludingDeleted(id)
                        .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));

                if (entity.isDeleted()) {
                    return false;
                }

                entity.setDeleted(true);
                entity.setDeletedAt(LocalDateTime.now());
                save(entity);

                return true;

            } catch (OptimisticLockException e) {
                retries++;

                if (retries >= maxRetries) {
                    throw new RuntimeException("Failed to soft delete after " + maxRetries + " retries", e);
                }

                try {
                    Thread.sleep(100L * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        return false;
    }

    @Query("select count(e) from #{#entityName} e where e.deleted = false")
    long countActive();
}
