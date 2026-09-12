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

    /**
     * Superadmin: acceso a configuraciones de "infraestructura del sitio"
     * (Cloudinary, servicio de mail) que no se gestionan como {@link Permission}
     * normal porque no deben poder auto-otorgarse desde `/admin/usuarios` (ABM de
     * roles). Sólo se setea sembrando la cuenta por variables de entorno
     * (`app.superadmin.*`) o directo en la base — nunca desde la UI de roles.
     */
    @Column(nullable = false)
    private boolean superAdmin = false;

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

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
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
