package com.springbank.user.repository;

import com.springbank.common.repository.BaseEntityRepository;
import com.springbank.user.entity.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends BaseEntityRepository<Role, Long> {

    // ========== Basic Queries ==========
    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    // ========== با Permissionها (Eager Loading) ==========
    @Query("SELECT r FROM roleEntity r LEFT JOIN FETCH r.permissions WHERE r.id = :id AND r.deleted = false")
    Optional<Role> findActiveByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT r FROM roleEntity r LEFT JOIN FETCH r.permissions WHERE r.name = :name AND r.deleted = false")
    Optional<Role> findActiveByNameWithPermissions(@Param("name") String name);

    // ========== Bulk Operations ==========
    Set<Role> findByNameIn(Set<String> names);

    @Query("SELECT r FROM roleEntity r WHERE r.deleted = false")
    Set<Role> findAllActiveRoles();

    // ========== Permission-based ==========
    @Query("""
        SELECT r FROM roleEntity r
        JOIN r.permissions p
        WHERE p.name = :permissionName AND r.deleted = false
    """)
    Set<Role> findAllActiveRolesByPermission(@Param("permissionName") String permissionName);

    // ========== User-based ==========
    @Query("""
        SELECT DISTINCT r FROM roleEntity r
        JOIN r.users u
        WHERE u.id = :userId AND r.deleted = false
    """)
    Set<Role> findAllActiveRolesByUserId(@Param("userId") Long userId);

    // ========== Counts ==========
    @Query("SELECT COUNT(u) FROM userEntity u JOIN u.roles r WHERE r.id = :roleId AND u.deleted = false")
    long countActiveUsersByRoleId(@Param("roleId") Long roleId);

    // ========== Default Role ==========
    @Query("SELECT r FROM roleEntity r WHERE r.name = 'ROLE_USER' AND r.deleted = false")
    Optional<Role> findDefaultUserRole();
}