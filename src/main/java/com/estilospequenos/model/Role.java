package com.estilospequenos.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Rol del panel: un nombre + un conjunto de {@link Permission}. El admin puede
 * crear roles y tildarles/destildarles permisos. El rol {@code system}
 * ("Superadmin") tiene todos los permisos siempre y no se puede editar ni
 * borrar. "Administrador" es un rol normal (editable) con un subconjunto fijo
 * de permisos — ver {@code RoleService.ensureRole}.
 */
@Entity
@Table(name = "role")
public class Role {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    /** true = rol de sistema ("Superadmin"): todos los permisos, no editable. */
    @Column(nullable = false)
    private boolean system = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 40)
    private Set<Permission> permissions = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Permisos efectivos: todos si es rol de sistema, si no los tildados. */
    public Set<Permission> effectivePermissions() {
        return system ? EnumSet.allOf(Permission.class) : EnumSet.copyOf(
                permissions.isEmpty() ? EnumSet.noneOf(Permission.class) : permissions);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
