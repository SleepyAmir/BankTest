package com.springbank.user.repository;

import com.springbank.user.entity.Permission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends BaseEntityRepository<Permission, Long> {

    // ========== Basic Queries ==========
    Optional<Permission> findByName(String name);
    
    boolean existsByName(String name);
    
    // ========== Bulk Operations ==========
    Set<Permission> findByNameIn(Set<String> names);
    
    @Query("SELECT p FROM permissionEntity p WHERE p.deleted = false")
    Set<Permission> findAllActivePermissions();
    
    // ========== با Prefix ==========
    @Query("SELECT p FROM permissionEntity p WHERE p.name LIKE CONCAT(:prefix, '%') AND p.deleted = false")
    Set<Permission> findAllActivePermissionsStartingWith(@Param("prefix") String prefix);
    
    // ========== Role-based ==========
    @Query("""
        SELECT DISTINCT p FROM permissionEntity p
        JOIN p.roles r
        WHERE r.id = :roleId AND p.deleted = false
    """)
    Set<Permission> findAllActivePermissionsByRoleId(@Param("roleId") Long roleId);
    
    @Query("""
        SELECT DISTINCT p FROM permissionEntity p
        JOIN p.roles r
        WHERE r.name = :roleName AND p.deleted = false
    """)
    Set<Permission> findAllActivePermissionsByRoleName(@Param("roleName") String roleName);
    
    // ========== User-based ==========
    @Query("""
        SELECT DISTINCT p FROM permissionEntity p
        JOIN p.roles r
        JOIN r.users u
        WHERE u.id = :userId AND p.deleted = false
    """)
    Set<Permission> findAllActivePermissionsByUserId(@Param("userId") Long userId);
    
    // ========== Validation ==========
    @Query("SELECT COUNT(p) > 0 FROM permissionEntity p WHERE p.name IN :permissionNames AND p.deleted = false")
    boolean existsAllPermissionsInSet(@Param("permissionNames") Set<String> permissionNames);
    
    // ========== Group by Category ==========
    @Query("""
        SELECT SUBSTRING(p.name, 1, LOCATE('_', p.name) - 1) as category,
               COUNT(p) as count
        FROM permissionEntity p
        WHERE p.deleted = false
        GROUP BY category
    """)
    List<Object[]> countPermissionsByCategory();
}