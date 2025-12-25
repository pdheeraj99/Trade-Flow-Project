package com.tradeflow.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Role entity for role-based access control (RBAC).
 * Predefined roles: USER, ADMIN
 */
@Entity
@Table(name = "roles", schema = "auth", indexes = @Index(name = "idx_roles_name", columnList = "name", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id", updatable = false, nullable = false)
    private UUID roleId;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;
}
