package com.estilospequenos.dto;

import com.estilospequenos.model.Permission;
import com.estilospequenos.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public final class RoleDtos {

    private RoleDtos() {}

    public record RoleRequest(
            @NotBlank @Size(max = 60) String name,
            Set<Permission> permissions
    ) {}

    public record RoleResponse(
            String id, String name, boolean system,
            List<String> permissions, long userCount
    ) {
        public static RoleResponse from(Role r, long userCount) {
            return new RoleResponse(
                    r.getId(), r.getName(), r.isSystem(),
                    r.effectivePermissions().stream().map(Enum::name).sorted().toList(),
                    userCount);
        }
    }

    /** Catálogo de permisos disponibles (para armar los checkboxes en el panel). */
    public record PermissionInfo(String key, String label) {
        public static PermissionInfo of(Permission p) {
            return new PermissionInfo(p.name(), p.getLabel());
        }
    }
}
