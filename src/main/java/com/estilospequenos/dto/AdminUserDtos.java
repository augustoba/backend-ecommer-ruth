package com.estilospequenos.dto;

import com.estilospequenos.model.AdminUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AdminUserDtos {

    private AdminUserDtos() {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 60) String username,
            @NotBlank @Size(min = 4, max = 100) String password,
            @NotBlank String roleId,
            Boolean enabled
    ) {}

    /** Edición: rol, estado y (opcional) contraseña nueva. */
    public record UpdateUserRequest(
            String roleId,
            Boolean enabled,
            @Size(min = 4, max = 100) String password
    ) {}

    public record UserResponse(
            String id, String username, String roleId, String roleName,
            boolean systemAdmin, boolean enabled, Instant createdAt
    ) {
        public static UserResponse from(AdminUser u) {
            return new UserResponse(
                    u.getId(), u.getUsername(),
                    u.getRole() != null ? u.getRole().getId() : null,
                    u.getRole() != null ? u.getRole().getName() : null,
                    u.isSystemAdmin(), u.isEnabled(), u.getCreatedAt());
        }
    }

    /** Respuesta de `/api/auth/me`: quién soy y qué puedo hacer. */
    public record MeResponse(String username, String roleName, boolean systemAdmin, java.util.List<String> permissions) {
        public static MeResponse from(AdminUser u) {
            return new MeResponse(
                    u.getUsername(),
                    u.getRole() != null ? u.getRole().getName() : null,
                    u.isSystemAdmin(),
                    u.permissions().stream().map(Enum::name).sorted().toList());
        }
    }
}
