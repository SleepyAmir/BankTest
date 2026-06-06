package com.springbank.account.entity;
import com.springbank.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Branch extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // کد یکتای شعبه
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(length = 15)
    private String phone;

    @Column(name = "manager_name", length = 100)
    private String managerName;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ======== RELATIONS ========

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    private List<Account> accounts;
}
