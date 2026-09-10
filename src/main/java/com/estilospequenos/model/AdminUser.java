package com.estilospequenos.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Usuario del panel de administración. La contraseña se guarda **hasheada
 * con BCrypt** (nunca en texto plano). El login (POST /api/auth/login) valida
 * contra esta tabla y devuelve un JWT.
 */
@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Hash BCrypt de la contraseña. */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * Hash BCrypt de la "frase de recuperación": un segundo secreto para poder
     * recuperar la cuenta si se olvida la contraseña, sin depender de email.
     */
    @Column
    private String recoveryHash;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Rol del usuario (define sus permisos). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public java.util.Set<Permission> permissions() {
        return role != null ? role.effectivePermissions()
                : java.util.EnumSet.noneOf(Permission.class);
    }

    public boolean isSystemAdmin() {
        return role != null && role.isSystem();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRecoveryHash() {
        return recoveryHash;
    }

    public void setRecoveryHash(String recoveryHash) {
        this.recoveryHash = recoveryHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
