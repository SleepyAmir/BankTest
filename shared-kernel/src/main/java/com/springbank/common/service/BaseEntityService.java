package com.springbank.common.service;

import com.springbank.common.entity.BaseEntity;
import com.springbank.common.repository.BaseEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseEntityService<T extends BaseEntity, ID extends Serializable, D> {

    protected final BaseEntityRepository<T, ID> repository;
    protected final Function<T, D> toDtoMapper;
    protected final Function<D, T> toEntityMapper;

    protected BaseEntityService(BaseEntityRepository<T, ID> repository,
                                Function<T, D> toDtoMapper,
                                Function<D, T> toEntityMapper) {
        this.repository = repository;
        this.toDtoMapper = toDtoMapper;
        this.toEntityMapper = toEntityMapper;
    }

    @Transactional(readOnly = true)
    public T findEntityById(@NonNull ID id) {
        log.debug("Finding {} entity by id: {}", getEntityTypeName(), id);
        return repository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("%s not found with id: %s", getEntityTypeName(), id)));
    }

    @Transactional(readOnly = true)
    public Optional<T> findEntityByIdOptional(@NonNull ID id) {
        log.debug("Finding {} entity optional by id: {}", getEntityTypeName(), id);
        return repository.findActiveById(id);
    }

    @Transactional(readOnly = true)
    public List<T> findAllEntities() {
        log.debug("Finding all active {} entities", getEntityTypeName());
        return repository.findAllActive();
    }

    @Transactional(readOnly = true)
    public D findDtoById(@NonNull ID id) {
        log.debug("Finding {} DTO by id: {}", getEntityTypeName(), id);
        T entity = findEntityById(id);
        return toDtoMapper.apply(entity);
    }

    @Transactional(readOnly = true)
    public Optional<D> findDtoByIdOptional(@NonNull ID id) {
        log.debug("Finding {} DTO optional by id: {}", getEntityTypeName(), id);
        return repository.findActiveById(id).map(toDtoMapper);
    }

    @Transactional(readOnly = true)
    public List<D> findAllDtos() {
        log.debug("Finding all {} DTOs", getEntityTypeName());
        return findAllEntities().stream()
                .map(toDtoMapper)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean existsByIdIncludingDeleted(@NonNull ID id) {
        return repository.findByIdIncludingDeleted(id).isPresent();
    }

    @Transactional(readOnly = true)
    public List<T> findDeletedEntities() {
        log.debug("Finding all deleted {} entities", getEntityTypeName());
        return repository.findAll().stream()
                .filter(BaseEntity::isDeleted)
                .collect(Collectors.toList());
    }

    @Transactional
    public T createEntity(@NonNull T entity) {
        log.info("Creating new {} entity", getEntityTypeName());
        T savedEntity = repository.save(entity);
        log.info("{} entity created successfully", getEntityTypeName());
        return savedEntity;
    }

    @Transactional
    public D createFromDto(@NonNull D dto) {
        log.info("Creating new {} from DTO", getEntityTypeName());
        T entity = toEntityMapper.apply(dto);
        T savedEntity = createEntity(entity);
        return toDtoMapper.apply(savedEntity);
    }

    @Transactional
    public T createEntityFromDto(@NonNull D dto) {
        log.info("Creating new {} entity from DTO", getEntityTypeName());
        T entity = toEntityMapper.apply(dto);
        return createEntity(entity);
    }

    @Transactional
    public List<T> createEntities(@NonNull List<T> entities) {
        log.info("Creating {} {} entities in bulk", entities.size(), getEntityTypeName());
        List<T> savedEntities = repository.saveAll(entities);
        log.info("{} entities created successfully", savedEntities.size());
        return savedEntities;
    }

    @Transactional
    public List<D> createFromDtos(@NonNull List<D> dtos) {
        log.info("Creating {} {} from DTOs in bulk", dtos.size(), getEntityTypeName());
        List<T> entities = dtos.stream()
                .map(toEntityMapper)
                .collect(Collectors.toList());
        List<T> savedEntities = createEntities(entities);
        return savedEntities.stream()
                .map(toDtoMapper)
                .collect(Collectors.toList());
    }

    @Transactional
    public T saveAndFlush(@NonNull T entity) {
        log.debug("Saving and flushing {} entity", getEntityTypeName());
        return repository.saveAndFlush(entity);
    }

    @Transactional
    public T updateEntity(@NonNull ID id, @NonNull Consumer<T> updateLogic) {
        log.debug("Updating {} entity with id: {}", getEntityTypeName(), id);
        return repository.updateWithConflictMessage(id, updateLogic);
    }

    @Transactional
    public D updateEntityAndReturnDto(@NonNull ID id, @NonNull Consumer<T> updateLogic) {
        T updatedEntity = updateEntity(id, updateLogic);
        return toDtoMapper.apply(updatedEntity);
    }

    @Transactional
    public D updateFromDto(@NonNull ID id, @NonNull Consumer<D> updateLogic) {
        log.debug("Updating {} from DTO with id: {}", getEntityTypeName(), id);
        T existingEntity = findEntityById(id);
        D dto = toDtoMapper.apply(existingEntity);
        updateLogic.accept(dto);
        T updatedEntity = toEntityMapper.apply(dto);
        copyCoreFields(existingEntity, updatedEntity);
        T savedEntity = repository.save(updatedEntity);
        log.debug("{} updated successfully from DTO with id: {}", getEntityTypeName(), id);
        return toDtoMapper.apply(savedEntity);
    }

    @Transactional
    public boolean softDelete(@NonNull ID id) {
        log.info("Soft deleting {} entity with id: {}", getEntityTypeName(), id);
        return repository.softDeleteWithRetry(id, 3);
    }

    @Transactional
    public boolean softDeleteWithRetry(@NonNull ID id, int maxRetries) {
        log.info("Soft deleting {} entity with id: {}, maxRetries: {}", getEntityTypeName(), id, maxRetries);
        return repository.softDeleteWithRetry(id, maxRetries);
    }

    @Transactional
    public boolean softDeleteWithVersionCheck(@NonNull ID id) {
        log.info("Soft deleting {} entity with version check, id: {}", getEntityTypeName(), id);
        T entity = repository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("%s not found with id: %s", getEntityTypeName(), id)));
        if (entity.isDeleted()) {
            log.warn("{} with id: {} is already deleted", getEntityTypeName(), id);
            return false;
        }
        int updatedRows = repository.softDeleteByIdWithVersion(
                id, LocalDateTime.now(), entity.getVersion());
        if (updatedRows == 0) {
            throw new java.util.ConcurrentModificationException(
                    String.format("%s with id: %s was modified by another user. Please refresh and try again.",
                            getEntityTypeName(), id)
            );
        }
        log.info("{} entity soft deleted successfully with id: {}", getEntityTypeName(), id);
        return true;
    }

    @Transactional
    public boolean restore(@NonNull ID id) {
        log.info("Restoring {} entity with id: {}", getEntityTypeName(), id);
        T entity = repository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("%s not found with id: %s", getEntityTypeName(), id)));
        if (!entity.isDeleted()) {
            log.warn("{} with id: {} is not deleted", getEntityTypeName(), id);
            return false;
        }
        entity.setDeleted(false);
        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        repository.save(entity);
        log.info("{} entity restored successfully", getEntityTypeName());
        return true;
    }

    @Transactional
    public void softDeleteBulk(@NonNull List<ID> ids) {
        log.info("Soft deleting {} {} entities in bulk", ids.size(), getEntityTypeName());
        ids.forEach(this::softDelete);
        log.info("{} entities soft deleted", ids.size());
    }

    @Transactional
    public void hardDelete(@NonNull ID id) {
        log.warn("Hard deleting {} entity with id: {}", getEntityTypeName(), id);
        T entity = findEntityById(id);
        repository.delete(entity);
        log.warn("{} entity hard deleted permanently", getEntityTypeName());
    }

    @Transactional
    public void hardDeleteBulk(@NonNull List<ID> ids) {
        log.warn("Hard deleting {} {} entities in bulk", ids.size(), getEntityTypeName());
        List<T> entities = ids.stream()
                .map(id -> repository.findByIdIncludingDeleted(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                String.format("%s not found with id: %s", getEntityTypeName(), id))))
                .collect(Collectors.toList());
        repository.deleteAll(entities);
        log.warn("{} entities hard deleted permanently", entities.size());
    }

    @Transactional(readOnly = true)
    public boolean exists(@NonNull ID id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsActive(@NonNull ID id) {
        return repository.findActiveById(id).isPresent();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return repository.countActive();
    }

    @Transactional(readOnly = true)
    public boolean existsByCondition(@NonNull Predicate<T> condition) {
        return findAllEntities().stream().anyMatch(condition);
    }

    @Transactional(readOnly = true)
    public void validateExists(@NonNull ID id) {
        if (!exists(id)) {
            throw new EntityNotFoundException(
                    String.format("%s not found with id: %s", getEntityTypeName(), id));
        }
    }

    @Transactional(readOnly = true)
    public void validateActive(@NonNull ID id) {
        if (!existsActive(id)) {
            throw new EntityNotFoundException(
                    String.format("Active %s not found with id: %s", getEntityTypeName(), id));
        }
    }

    protected abstract String getEntityTypeName();

    protected void copyCoreFields(@NonNull T source, @NonNull T target) {
        log.debug("Copying core fields from {} to {}", getEntityTypeName(), getEntityTypeName());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setDeleted(source.isDeleted());
        target.setDeletedAt(source.getDeletedAt());
        target.setDeletedBy(source.getDeletedBy());
        target.setVersion(source.getVersion());
    }

    protected D toDto(@NonNull T entity) {
        return toDtoMapper.apply(entity);
    }

    protected T toEntity(@NonNull D dto) {
        return toEntityMapper.apply(dto);
    }

    protected List<D> toDtoList(@NonNull List<T> entities) {
        return entities.stream()
                .map(toDtoMapper)
                .collect(Collectors.toList());
    }

    protected List<T> toEntityList(@NonNull List<D> dtos) {
        return dtos.stream()
                .map(toEntityMapper)
                .collect(Collectors.toList());
    }

    protected Optional<D> mapToDtoOptional(T entity) {
        return Optional.ofNullable(entity).map(toDtoMapper);
    }

    protected Optional<T> mapToEntityOptional(D dto) {
        return Optional.ofNullable(dto).map(toEntityMapper);
    }

    protected BaseEntityRepository<T, ID> getRepository() {
        return repository;
    }
}
