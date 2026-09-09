package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.RoleDtos.RoleRequest;
import com.estilospequenos.model.Permission;
import com.estilospequenos.model.Role;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.RoleRepository;
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
        return repo.findAllByOrderBySystemDescNameAsc();
    }

    @Transactional(readOnly = true)
    public Role get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
    }

    public long userCount(String roleId) {
        return users.countByRoleId(roleId);
    }

    public Role create(RoleRequest req) {
        String name = req.name().trim();
        if (repo.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Ya existe un rol con ese nombre.");
        }
        Role r = new Role();
        r.setId(UUID.randomUUID().toString());
        r.setName(name);
        r.setSystem(false);
        r.setPermissions(cleanPermissions(req));
        return repo.save(r);
    }

    public Role update(String id, RoleRequest req) {
        Role r = get(id);
        if (r.isSystem()) {
            throw new BadRequestException("El rol Administrador no se puede editar (tiene todos los permisos).");
        }
        String name = req.name().trim();
        if (!name.equalsIgnoreCase(r.getName()) && repo.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Ya existe un rol con ese nombre.");
        }
        r.setName(name);
        r.setPermissions(cleanPermissions(req));
        return repo.save(r);
    }

    public void delete(String id) {
        Role r = get(id);
        if (r.isSystem()) {
            throw new BadRequestException("El rol Administrador no se puede borrar.");
        }
        if (users.countByRoleId(id) > 0) {
            throw new BadRequestException("Hay usuarios con este rol. Cambiales el rol antes de borrarlo.");
        }
        repo.delete(r);
    }

    /** Crea los roles iniciales si no existen (se llama desde el DataSeeder). */
    public Role ensureSystemRole() {
        return repo.findFirstBySystemTrue().orElseGet(() -> {
            Role admin = new Role();
            admin.setId(UUID.randomUUID().toString());
            admin.setName("Administrador");
            admin.setSystem(true);
            admin.setPermissions(EnumSet.allOf(Permission.class));
            return repo.save(admin);
        });
    }

    public void ensureRole(String name, Permission... perms) {
        if (repo.findByNameIgnoreCase(name).isEmpty()) {
            Role r = new Role();
            r.setId(UUID.randomUUID().toString());
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
