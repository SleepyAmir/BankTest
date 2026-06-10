package com.springbank.user.service;

import com.springbank.common.service.BaseEntityService;
import com.springbank.user.dto.*;
import com.springbank.user.mapper.UserMapper;
import com.springbank.user.entity.Role;
import com.springbank.user.entity.User;
import com.springbank.user.repository.RoleRepository;
import com.springbank.user.repository.UserRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserService extends BaseEntityService<User, Long, UserResponseDto> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final int PASSWORD_EXPIRY_DAYS = 90;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        super(userRepository,
                userMapper::toResponseDto,
                dto -> {
                    throw new UnsupportedOperationException(
                            "Use registerUser method instead of createFromDto"
                    );
                });
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected String getEntityTypeName() {
        return "User";
    }

    // ========== Create Operations ==========

    @Transactional
    public UserResponseDto registerUser(@NonNull UserRegistrationDto registrationDto) {
        log.info("✨ Registering new user: {}", registrationDto.username());

        validateNewUser(registrationDto.username(), registrationDto.email());

        Role defaultRole = roleRepository.findDefaultUserRole()
                .orElseThrow(() -> new IllegalStateException("Default role not found in system"));

        User user = User.builder()
                .username(registrationDto.username())
                .email(registrationDto.email())
                .password(passwordEncoder.encode(registrationDto.password()))
                .firstName(registrationDto.firstName())
                .lastName(registrationDto.lastName())
                .phoneNumber(registrationDto.phoneNumber())
                .profilePictureUrl(registrationDto.profilePictureUrl())
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .emailVerified(false)
                .twoFactorEnabled(false)
                .failedAttempts(0)
                .passwordChangedAt(LocalDateTime.now())
                .roles(new HashSet<>(Set.of(defaultRole)))
                .build();

        User savedUser = createEntity(user);
        log.info("✅ User registered successfully with id: {}", savedUser.getId());

        return userMapper.toResponseDto(savedUser);
    }

    // ========== Read Operations ==========

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(@NonNull Long id) {
        log.debug("🔍 Finding user by id: {}", id);
        return findDtoById(id);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByUsername(@NonNull String username) {
        log.debug("🔍 Finding user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(@NonNull String email) {
        log.debug("🔍 Finding user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public Set<UserResponseDto> getAllUsers() {
        log.debug("📋 Fetching all active users");
        return Set.copyOf(findAllDtos());
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(@NonNull Long id) {
        return findEntityById(id);
    }

    @Transactional(readOnly = true)
    public User getUserEntityByUsername(@NonNull String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // ========== Update Operations ==========

    @Transactional
    public UserResponseDto updateUser(@NonNull Long userId, @NonNull UserUpdateDto updateDto) {
        log.info("✏️ Updating user with id: {}", userId);

        User user = findEntityById(userId);

        if (updateDto.email() != null && !updateDto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDto.email())) {
                throw new IllegalArgumentException("Email already exists: " + updateDto.email());
            }
            user.setEmail(updateDto.email());
            user.setEmailVerified(false);
        }

        UserResponseDto responseDto = updateEntityAndReturnDto(userId, u -> userMapper.updateUserFromDto(updateDto, u));

        log.info("✅ User updated successfully: {}", responseDto.username());
        return responseDto;
    }

    @Transactional
    public void changePassword(@NonNull Long userId, @NonNull ChangePasswordDto passwordDto) {
        log.info("🔐 Changing password for user id: {}", userId);

        User user = findEntityById(userId);

        if (!passwordEncoder.matches(passwordDto.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(passwordDto.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setCredentialsNonExpired(true);
        repository.save(user);

        log.info("✅ Password changed successfully for user: {}", user.getUsername());
    }

    @Transactional
    public void resetPassword(@NonNull Long userId, @NonNull String newPassword) {
        log.info("🔐 Resetting password for user id: {}", userId);

        User user = findEntityById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setCredentialsNonExpired(true);
        repository.save(user);

        log.info("✅ Password reset successfully for user: {}", user.getUsername());
    }

    // ========== Role Management ==========

    @Transactional
    public UserResponseDto toggleStatus(Long userId, boolean enabled) {
        log.info("🔒 Toggling user status. userId: {}, enabled: {}", userId, enabled);
        User user = findEntityById(userId);
        user.setEnabled(enabled);
        User updated = userRepository.save(user);
        return userMapper.toResponseDto(updated);
    }

    @Transactional
    public UserResponseDto assignRolesToUser(@NonNull Long userId, @NonNull Set<Long> roleIds) {
        log.info("🔗 Assigning {} roles to user id: {}", roleIds.size(), userId);

        User user = findEntityById(userId);
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));

        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = roleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Some role IDs are invalid: " + missingIds);
        }

        user.setRoles(roles);
        User savedUser = repository.save(user);

        log.info("✅ Roles assigned successfully to user: {}", user.getUsername());
        return userMapper.toResponseDto(savedUser);
    }

    @Transactional
    public UserResponseDto addRoleToUser(@NonNull Long userId, @NonNull Long roleId) {
        log.info("🔗 Adding role {} to user {}", roleId, userId);

        User user = findEntityById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        user.getRoles().add(role);
        User savedUser = repository.save(user);

        log.info("✅ Role {} added to user {}", role.getName(), user.getUsername());
        return userMapper.toResponseDto(savedUser);
    }

    @Transactional
    public UserResponseDto removeRoleFromUser(@NonNull Long userId, @NonNull Long roleId) {
        log.info("🔗 Removing role {} from user {}", roleId, userId);

        User user = findEntityById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        user.getRoles().remove(role);
        User savedUser = repository.save(user);

        log.info("✅ Role {} removed from user {}", role.getName(), user.getUsername());
        return userMapper.toResponseDto(savedUser);
    }

    // ========== Account Status Management ==========

    @Transactional
    public UserResponseDto enableUser(@NonNull Long userId) {
        log.info("🔓 Enabling user with id: {}", userId);
        return updateEntityAndReturnDto(userId, user -> user.setEnabled(true));
    }

    @Transactional
    public UserResponseDto disableUser(@NonNull Long userId) {
        log.info("🔒 Disabling user with id: {}", userId);
        return updateEntityAndReturnDto(userId, user -> user.setEnabled(false));
    }

    @Transactional
    public UserResponseDto unlockUser(@NonNull Long userId) {
        log.info("🔓 Unlocking user with id: {}", userId);
        return updateEntityAndReturnDto(userId, user -> {
            user.setAccountNonLocked(true);
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        });
    }

    @Transactional
    public UserResponseDto verifyEmail(@NonNull Long userId) {
        log.info("✅ Verifying email for user id: {}", userId);
        return updateEntityAndReturnDto(userId, user -> user.setEmailVerified(true));
    }

    // ========== Login Attempts Management ==========

    @Transactional
    public void recordSuccessfulLogin(@NonNull String username, @NonNull String ipAddress) {
        log.debug("✅ Recording successful login for user: {}", username);

        userRepository.findByUsername(username).ifPresent(user -> {
            user.recordSuccessfulLogin(ipAddress);
            repository.save(user);
        });
    }

    @Transactional
    public void recordFailedLogin(@NonNull String username) {
        log.debug("❌ Recording failed login for user: {}", username);

        userRepository.findByUsername(username).ifPresent(user -> {
            user.recordFailedLogin(MAX_FAILED_ATTEMPTS, LOCK_DURATION_MINUTES);
            repository.save(user);

            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                log.warn("⚠️ User {} has been locked due to too many failed attempts", username);
            }
        });
    }

    // ========== Delete Operations ==========

    @Transactional
    public void deleteUser(@NonNull Long userId) {
        log.info("🗑️ Deleting user with id: {}", userId);

        User user = findEntityById(userId);

        if ("admin".equals(user.getUsername())) {
            throw new IllegalStateException("Cannot delete the main admin user");
        }

        softDelete(userId);
        log.info("✅ User deleted successfully: {}", user.getUsername());
    }

    @Transactional
    public void hardDeleteUser(@NonNull Long userId) {
        log.warn("⚠️ Hard deleting user with id: {}", userId);

        User user = findEntityById(userId);
        if ("admin".equals(user.getUsername())) {
            throw new IllegalStateException("Cannot delete the main admin user");
        }

        hardDelete(userId);
        log.warn("🗑️ User hard deleted: {}", user.getUsername());
    }

    // ========== Validation & Helper Methods ==========

    private void validateNewUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
    }

    @Transactional(readOnly = true)
    public boolean isPasswordExpired(@NonNull Long userId) {
        User user = findEntityById(userId);
        return user.isPasswordExpired(PASSWORD_EXPIRY_DAYS);
    }

    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return countActive();
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(@NonNull String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(@NonNull String email) {
        return userRepository.existsByEmail(email);
    }

    // ========== Additional Queries from Repository ==========

    @Transactional(readOnly = true)
    public List<UserResponseDto> findDisabledUsers() {
        log.debug("🔍 Finding disabled users");
        return userRepository.findDisabledUsers().stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findLockedUsers() {
        log.debug("🔍 Finding locked users");
        return userRepository.findLockedUsers().stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findInactiveUsers(@NonNull LocalDateTime date) {
        log.debug("🔍 Finding inactive users since: {}", date);
        return userRepository.findInactiveUsers(date).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findUnverifiedUsersOlderThan(@NonNull LocalDateTime dateTime) {
        log.debug("🔍 Finding unverified users older than: {}", dateTime);
        return userRepository.findUnverifiedUsersOlderThan(dateTime).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findAllActiveUsersByRoleName(@NonNull String roleName) {
        log.debug("🔍 Finding users by role name: {}", roleName);
        return userRepository.findAllActiveUsersByRoleName(roleName).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findAllActiveUsersWithPermission(@NonNull String permissionName) {
        log.debug("🔍 Finding users with permission: {}", permissionName);
        return userRepository.findAllActiveUsersWithPermission(permissionName).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public int disableInactiveUsers(@NonNull LocalDateTime cutoffDate) {
        log.info("🔒 Disabling inactive users since: {}", cutoffDate);
        int count = userRepository.disableInactiveUsers(cutoffDate);
        log.info("✅ {} users disabled", count);
        return count;
    }
}