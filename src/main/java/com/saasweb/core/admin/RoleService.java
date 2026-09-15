package com.saasweb.core.admin;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.admin.RoleDtos.RoleRequest;
import com.saasweb.core.admin.Permission;
import com.saasweb.core.admin.Role;
import com.saasweb.core.admin.AdminUserRepository;
import com.saasweb.core.admin.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RoleService {

    private final RoleRepository repo;
    private final AdminUserRepository users;

    public RoleService(RoleRepository repo, AdminUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return repo.findAllForTenant(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Role get(String id) {
        return repo.findByIdForTenant(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
    }

    public long userCount(String roleId) {
        return users.countByRoleId(roleId);
    }

    public Role create(RoleRequest req) {
        String tenantId = TenantContext.getTenantId();
        String name = req.name().trim();
        if (repo.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new BadRequestException("Ya existe un rol con ese nombre.");
        }
        Role r = new Role();
        r.setId(UUID.randomUUID().toString());
        r.setTenantId(tenantId);
        r.setName(name);
        r.setSystem(false);
        r.setPermissions(cleanPermissions(req));
        return repo.save(r);
    }

    public Role update(String id, RoleRequest req) {
        Role r = get(id);
        if (r.isSystem()) {
            throw new BadRequestException("El rol Superadmin no se puede editar (tiene todos los permisos).");
        }
        String name = req.name().trim();
        if (!name.equalsIgnoreCase(r.getName()) && repo.existsByTenantIdAndNameIgnoreCase(r.getTenantId(), name)) {
            throw new BadRequestException("Ya existe un rol con ese nombre.");
        }
        r.setName(name);
        r.setPermissions(cleanPermissions(req));
        return repo.save(r);
    }

    public void delete(String id) {
        Role r = get(id);
        if (r.isSystem()) {
            throw new BadRequestException("El rol Superadmin no se puede borrar.");
        }
        if (users.countByRoleId(id) > 0) {
            throw new BadRequestException("Hay usuarios con este rol. Cambiales el rol antes de borrarlo.");
        }
        repo.delete(r);
    }

    /**
     * Rol del sistema: siempre TODOS los permisos (incluidos los que se agreguen a
     * futuro), no se puede editar ni borrar. Pensado para el desarrollador/dueño de
     * la plataforma, no para el admin de cada tienda (ver {@link #ensureRole}).
     * Se llama desde el DataSeeder.
     */
    public Role ensureSystemRole() {
        return repo.findFirstBySystemTrue().orElseGet(() -> {
            Role superadmin = new Role();
            superadmin.setId(UUID.randomUUID().toString());
            superadmin.setName("Superadmin");
            superadmin.setSystem(true);
            superadmin.setPermissions(EnumSet.allOf(Permission.class));
            return repo.save(superadmin);
        });
    }

    public void ensureRole(String tenantId, String name, Permission... perms) {
        if (repo.findByTenantIdAndNameIgnoreCase(tenantId, name).isEmpty()) {
            Role r = new Role();
            r.setId(UUID.randomUUID().toString());
            r.setTenantId(tenantId);
            r.setName(name);
            r.setSystem(false);
            r.setPermissions(perms.length == 0
                    ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(List.of(perms)));
            repo.save(r);
        }
    }

    private java.util.Set<Permission> cleanPermissions(RoleRequest req) {
        return req.permissions() == null || req.permissions().isEmpty()
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(req.permissions());
    }
}
