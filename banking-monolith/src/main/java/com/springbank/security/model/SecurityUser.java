package com.springbank.security.model;

import com.springbank.user.entity.Permission;
import com.springbank.user.entity.Role;
import com.springbank.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * مدل امنیتی کاربر برای Spring Security
 * این کلاس به عنوان یک آداپتور بین Entity اصلی User و نیازهای Spring Security عمل می‌کند
 */
@Getter
public final class SecurityUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // =========================================================================
    // ============================ FIELDS =====================================
    // =========================================================================

    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final LocalDateTime lockedUntil;
    private final Collection<GrantedAuthority> authorities;

    // حافظه کش برای مقادیر محاسباتی
    private transient String fullNameCache;
    private transient Boolean hasAdminRoleCache;

    // =========================================================================
    // ============================ CONSTRUCTOR ================================
    // =========================================================================

    public SecurityUser(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.enabled = user.isEnabled() && !user.isDeleted();
        this.accountNonExpired = user.isAccountNonExpired();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.lockedUntil = user.getLockedUntil();
        this.accountNonLocked = calculateAccountNonLocked(user);
        this.authorities = extractAuthorities(user);
    }

    // =========================================================================
    // ============================ PRIVATE METHODS ============================
    // =========================================================================

    private boolean calculateAccountNonLocked(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return false;
        }
        return user.isAccountNonLocked();
    }

    private Collection<GrantedAuthority> extractAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        return Set.copyOf(authorities);
    }

    // =========================================================================
    // ============================ USER DETAILS METHODS ========================
    // =========================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // =========================================================================
    // ============================ HELPER METHODS =============================
    // =========================================================================

    public String getFullName() {
        if (fullNameCache == null) {
            if (firstName == null && lastName == null) {
                fullNameCache = username;
            } else if (firstName == null) {
                fullNameCache = lastName;
            } else if (lastName == null) {
                fullNameCache = firstName;
            } else {
                fullNameCache = firstName + " " + lastName;
            }
        }
        return fullNameCache;
    }

    public boolean hasRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        return authorities.stream()
                .anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null) {
            return false;
        }
        return authorities.stream()
                .anyMatch(authority -> permissionName.equals(authority.getAuthority()));
    }

    public boolean isAdmin() {
        if (hasAdminRoleCache == null) {
            hasAdminRoleCache = hasRole("ROLE_ADMIN") || hasRole("ROLE_SUPER_ADMIN");
        }
        return hasAdminRoleCache;
    }

    // =========================================================================
    // ============================ EQUALS & HASHCODE ==========================
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SecurityUser that = (SecurityUser) obj;
        return Objects.equals(id, that.id) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "SecurityUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", accountNonLocked=" + accountNonLocked +
                '}';
    }
}