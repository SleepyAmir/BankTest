package com.springbank.config;

import com.springbank.user.entity.Permission;
import com.springbank.user.entity.Role;
import com.springbank.user.repository.PermissionRepository;
import com.springbank.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("🔧 Initializing default data...");
        initPermissions();
        initRoles();
        log.info("✅ Default data initialized successfully.");
    }

    private void initPermissions() {
        createPermissionIfNotExists("READ_ACCOUNT", "Read account data", "ACCOUNT");
        createPermissionIfNotExists("WRITE_ACCOUNT", "Write account data", "ACCOUNT");
        createPermissionIfNotExists("READ_TRANSACTION", "Read transaction data", "TRANSACTION");
        createPermissionIfNotExists("WRITE_TRANSACTION", "Write transaction data", "TRANSACTION");
        createPermissionIfNotExists("READ_LOAN", "Read loan data", "LOAN");
        createPermissionIfNotExists("WRITE_LOAN", "Write loan data", "LOAN");
        createPermissionIfNotExists("READ_CARD", "Read card data", "CARD");
        createPermissionIfNotExists("WRITE_CARD", "Write card data", "CARD");
        createPermissionIfNotExists("READ_USER", "Read user data", "USER");
        createPermissionIfNotExists("WRITE_USER", "Write user data", "USER");
        createPermissionIfNotExists("READ_NOTIFICATION", "Read notifications", "NOTIFICATION");
        createPermissionIfNotExists("ADMIN_ACCESS", "Full admin access", "ADMIN");
    }

    private void initRoles() {
        // ROLE_USER
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Set<Permission> userPermissions = Set.of(
                    permissionRepository.findByName("READ_ACCOUNT").orElseThrow(),
                    permissionRepository.findByName("READ_TRANSACTION").orElseThrow(),
                    permissionRepository.findByName("READ_LOAN").orElseThrow(),
                    permissionRepository.findByName("READ_CARD").orElseThrow(),
                    permissionRepository.findByName("READ_NOTIFICATION").orElseThrow()
            );
            Role userRole = Role.builder()
                    .name("ROLE_USER")
                    .description("Default user role")
                    .priority(0)
                    .permissions(new java.util.HashSet<>(userPermissions))
                    .build();
            roleRepository.save(userRole);
            log.info("✅ ROLE_USER created");
        }

        // ROLE_ADMIN
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Set<Permission> allPermissions = new java.util.HashSet<>(
                    permissionRepository.findAllActive()
            );
            Role adminRole = Role.builder()
                    .name("ROLE_ADMIN")
                    .description("Administrator role")
                    .priority(10)
                    .permissions(allPermissions)
                    .build();
            roleRepository.save(adminRole);
            log.info("✅ ROLE_ADMIN created");
        }

        // ROLE_CUSTOMER_SERVICE
        if (roleRepository.findByName("ROLE_CUSTOMER_SERVICE").isEmpty()) {
            Set<Permission> csPermissions = Set.of(
                    permissionRepository.findByName("READ_ACCOUNT").orElseThrow(),
                    permissionRepository.findByName("WRITE_ACCOUNT").orElseThrow(),
                    permissionRepository.findByName("READ_USER").orElseThrow(),
                    permissionRepository.findByName("READ_LOAN").orElseThrow(),
                    permissionRepository.findByName("READ_CARD").orElseThrow()
            );
            Role csRole = Role.builder()
                    .name("ROLE_CUSTOMER_SERVICE")
                    .description("Customer service role")
                    .priority(5)
                    .permissions(new java.util.HashSet<>(csPermissions))
                    .build();
            roleRepository.save(csRole);
            log.info("✅ ROLE_CUSTOMER_SERVICE created");
        }
    }

    private void createPermissionIfNotExists(String name, String description, String category) {
        if (permissionRepository.findByName(name).isEmpty()) {
            Permission permission = Permission.builder()
                    .name(name)
                    .description(description)
                    .category(category)
                    .systemDefault(true)
                    .build();
            permissionRepository.save(permission);
            log.info("✅ Permission created: {}", name);
        }
    }
}