package com.springbank.user.repository;

import com.springbank.common.repository.BaseEntityRepository;
import com.springbank.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends BaseEntityRepository<User, Long> {

    // ========== Basic Queries ==========

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


    // ========== Status Queries ==========

    List<User> findByEnabledTrue();

    List<User> findByEnabledFalse();

    @Query("SELECT u FROM userEntity u WHERE u.enabled = false AND u.deleted = false")
    List<User> findDisabledUsers();

    @Query("SELECT u FROM userEntity u WHERE u.accountNonLocked = false AND u.deleted = false")
    List<User> findLockedUsers();

    @Query("SELECT u FROM userEntity u WHERE u.lastLogin < :date AND u.enabled = true")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM userEntity u WHERE u.emailVerified = false AND u.createdAt < :dateTime")
    List<User> findUnverifiedUsersOlderThan(@Param("dateTime") LocalDateTime dateTime);


    // ========== Role-based Queries ==========

    @Query("SELECT u FROM userEntity u JOIN u.roles r WHERE r.name = :roleName AND u.deleted = false")
    List<User> findAllActiveUsersByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM userEntity u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findAllUsersByRoleNames(@Param("roleNames") Set<String> roleNames);

    @Query("SELECT COUNT(u) FROM userEntity u JOIN u.roles r WHERE r.id = :roleId AND u.deleted = false")
    long countActiveUsersByRoleId(@Param("roleId") Long roleId);


    // ========== Permission-based Queries ==========

    @Query("""
        SELECT DISTINCT u FROM userEntity u
        JOIN u.roles r
        JOIN r.permissions p
        WHERE p.name = :permissionName AND u.deleted = false
    """)
    List<User> findAllActiveUsersWithPermission(@Param("permissionName") String permissionName);


    // ========== Complex Queries (JOIN FETCH for Profile) ==========

    @Query("SELECT DISTINCT u FROM userEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions p " +
            "WHERE u.id = :id AND u.deleted = false")
    Optional<User> findActiveByIdWithRolesAndPermissions(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM userEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions p " +
            "WHERE u.username = :username AND u.deleted = false")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);


    // ========== Login & Security Queries ==========

    @Modifying
    @Transactional
    @Query("UPDATE userEntity u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.username = :username")
    void incrementFailedAttempts(@Param("username") String username);

    @Modifying
    @Transactional
    @Query("UPDATE userEntity u SET u.lockedUntil = :lockedUntil WHERE u.username = :username")
    void lockUserAccount(@Param("username") String username, @Param("lockedUntil") LocalDateTime lockedUntil);


    // ========== Pagination ==========

    Page<User> findAllByDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM userEntity u WHERE u.deleted = false AND u.enabled = true")
    Page<User> findAllActive(Pageable pageable);


    // ========== Search ==========

    @Query("""
        SELECT u FROM userEntity u
        WHERE u.deleted = false
        AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);


    // ========== Counts ==========

    long countByDeletedFalseAndEnabledTrue();

    @Query("SELECT COUNT(u) FROM userEntity u JOIN u.roles r WHERE r.name = :roleName AND u.deleted = false")
    long countActiveUsersByRole(@Param("roleName") String roleName);


    // ========== Projections ==========

    @Query("SELECT u.id, u.username, u.email, u.firstName, u.lastName FROM userEntity u WHERE u.deleted = false")
    List<UserSummary> findAllUserSummaries();

    interface UserSummary {
        Long getId();
        String getUsername();
        String getEmail();
        String getFirstName();
        String getLastName();
    }


    // ========== Bulk Operations ==========

    @Modifying
    @Transactional
    @Query("UPDATE userEntity u SET u.enabled = false WHERE u.lastLogin < :cutoffDate AND u.enabled = true")
    int disableInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
}