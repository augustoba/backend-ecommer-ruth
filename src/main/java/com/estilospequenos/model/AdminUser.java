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

    /**
     * Identificador de login. Para cuentas nuevas es el **DNI** (así lo pidió el
     * cliente: "que se logeen con su dni") — el form de alta lo etiqueta "DNI",
     * pero el campo/columna sigue llamándose `username` para no romper el login,
     * el JWT (`sub`) ni las cuentas viejas que todavía tienen un username libre
     * (ej. la cuenta "admin" original).
     */
    @Column(nullable = false, unique = true)
    private String username;

    /** Nombre de pila. Opcional para no romper cuentas viejas sin backfillear. */
    @Column(length = 100)
    private String firstName;

    /** Apellido. */
    @Column(length = 100)
    private String lastName;

    /**
     * Mail para recuperar la cuenta. Hoy la recuperación sigue siendo por
     * `recoveryHash` (frase secreta) — este campo queda guardado para cuando se
     * conecte el envío de mail (falta decidir proveedor SMTP).
     */
    @Column(length = 200)
    private String email;

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

    /**
     * Superadmin: acceso a configuraciones de "infraestructura del sitio"
     * (hoy: credenciales de Cloudinary) que no se gestionan como {@link Permission}
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
