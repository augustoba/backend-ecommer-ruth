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

    /** Documento de identidad: es el identificador de login (reemplaza al username viejo). */
    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt de la contraseña. */
    @Column(nullable = false)
    private String passwordHash;

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
}
