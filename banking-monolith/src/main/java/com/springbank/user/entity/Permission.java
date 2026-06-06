package com.springbank.user.entity;
import com.springbank.common.entity.BaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity(name="permissionEntity")
@Table(name="permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permission_name", columnNames = "name"),
        indexes = {
                @Index(name = "idx_permission_name", columnList = "name"),
                @Index(name = "idx_permission_deleted", columnList = "deleted"),
        })
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class Permission extends BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column
    private String description;

    @Column(length = 50)
    private String category;

    @Builder.Default
    private boolean systemDefault = false;

    @ManyToMany(mappedBy = "permissions")
    @JsonIgnore
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public String getAuthority() {
        return name;
    }
}