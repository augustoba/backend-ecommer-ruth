package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Usuario del panel de administración. La contraseña se guarda **hasheada
 * con BCrypt** (nunca en texto plano). El login (POST /api/auth/login) valida
 * contra esta tabla y devuelve un JWT.
 */
@Entity
@Table(name = "admin_user")
@Getter
@Setter
@NoArgsConstructor
public class AdminUser {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Hash BCrypt de la contraseña. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
