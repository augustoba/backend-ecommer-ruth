package com.estilospequenos.dto;

import com.estilospequenos.model.AdminUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AdminUserDtos {

    private AdminUserDtos() {}

    /** `username` es el DNI para cuentas nuevas (ver comentario en AdminUser). */
    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 60) String username,
            @NotBlank @Size(min = 4, max = 100) String password,
            @NotBlank String roleId,
            Boolean enabled,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 200) String email
    ) {}

    /** Edición: rol, estado, datos personales y (opcional) contraseña nueva. */
    public record UpdateUserRequest(
            String roleId,
            Boolean enabled,
            @Size(min = 4, max = 100) String password,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 200) String email
    ) {}

    public record UserResponse(
            String id, String username, String roleId, String roleName,
            boolean systemAdmin, boolean enabled, Instant createdAt,
            String firstName, String lastName, String email
    ) {
        public static UserResponse from(AdminUser u) {
            return new UserResponse(
                    u.getId(), u.getUsername(),
                    u.getRole() != null ? u.getRole().getId() : null,
                    u.getRole() != null ? u.getRole().getName() : null,
                    u.isSystemAdmin(), u.isEnabled(), u.getCreatedAt(),
                    u.getFirstName(), u.getLastName(), u.getEmail());
        }
    }

    /** Respuesta de `/api/auth/me`: quién soy y qué puedo hacer. */
    public record MeResponse(
            String username, String roleName, boolean systemAdmin,
            boolean superAdmin, java.util.List<String> permissions,
            String firstName, String lastName
    ) {
        public static MeResponse from(AdminUser u) {
            return new MeResponse(
                    u.getUsername(),
                    u.getRole() != null ? u.getRole().getName() : null,
                    u.isSystemAdmin(),
                    u.isSuperAdmin(),
                    u.permissions().stream().map(Enum::name).sorted().toList(),
                    u.getFirstName(), u.getLastName());
        }
    }
}
