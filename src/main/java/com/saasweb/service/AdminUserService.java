package com.saasweb.service;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.dto.AdminUserDtos.CreateUserRequest;
import com.saasweb.dto.AdminUserDtos.UpdateUserRequest;
import com.saasweb.model.AdminUser;
import com.saasweb.model.Role;
import com.saasweb.repository.AdminUserRepository;
import com.saasweb.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Alta / edición de usuarios del panel (distinto de AccountService, que es self-service). */
@Service
@Transactional
public class AdminUserService {

    private final AdminUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AdminUserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUser> findAll() {
        return users.findByTenantIdOrderByCreatedAtAsc(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public AdminUser get(String id) {
        return users.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    public AdminUser create(CreateUserRequest req, String actingDni) {
        String tenantId = TenantContext.getTenantId();
        String dni = req.dni().trim();
        if (users.existsByTenantIdAndDniIgnoreCase(tenantId, dni)) {
            throw new BadRequestException("Ya existe un usuario con ese DNI.");
        }
        if (users.existsByTenantIdAndEmailIgnoreCase(tenantId, req.email().trim())) {
            throw new BadRequestException("Ya existe un usuario con ese email.");
        }
        Role role = roles.findByIdForTenant(req.roleId(), tenantId)
                .orElseThrow(() -> new BadRequestException("El rol elegido no existe."));
        assertCanAssign(role, actingDni);

        AdminUser u = new AdminUser();
        u.setId(UUID.randomUUID().toString());
        u.setTenantId(tenantId);
        u.setDni(dni);
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setEmail(req.email().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(role);
        u.setEnabled(req.enabled() == null || req.enabled());
        return users.save(u);
    }

    public AdminUser update(String id, UpdateUserRequest req, String actingDni) {
        AdminUser u = get(id);
        boolean editingSelf = u.getDni().equalsIgnoreCase(actingDni);

        if (req.roleId() != null && !req.roleId().isBlank()) {
            Role role = roles.findByIdForTenant(req.roleId(), TenantContext.getTenantId())
                    .orElseThrow(() -> new BadRequestException("El rol elegido no existe."));
            if (editingSelf && u.isSystemAdmin() && !role.isSystem()) {
                throw new BadRequestException("No podés sacarte a vos mismo el rol de Superadmin.");
            }
            assertCanAssign(role, actingDni);
            u.setRole(role);
        }
        if (req.enabled() != null) {
            if (editingSelf && !req.enabled()) {
                throw new BadRequestException("No podés deshabilitar tu propio usuario.");
            }
            u.setEnabled(req.enabled());
        }
        if (req.nombre() != null && !req.nombre().isBlank()) u.setNombre(req.nombre().trim());
        if (req.apellido() != null && !req.apellido().isBlank()) u.setApellido(req.apellido().trim());
        if (req.dni() != null && !req.dni().isBlank() && !req.dni().trim().equalsIgnoreCase(u.getDni())) {
            if (users.existsByTenantIdAndDniIgnoreCase(TenantContext.getTenantId(), req.dni().trim())) {
                throw new BadRequestException("Ya existe un usuario con ese DNI.");
            }
            u.setDni(req.dni().trim());
        }
        if (req.email() != null && !req.email().isBlank() && !req.email().trim().equalsIgnoreCase(u.getEmail())) {
            if (users.existsByTenantIdAndEmailIgnoreCase(TenantContext.getTenantId(), req.email().trim())) {
                throw new BadRequestException("Ya existe un usuario con ese email.");
            }
            u.setEmail(req.email().trim());
        }
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return users.save(u);
    }

    public void delete(String id, String actingDni) {
        AdminUser u = get(id);
        if (u.getDni().equalsIgnoreCase(actingDni)) {
            throw new BadRequestException("No podés borrar tu propio usuario.");
        }
        if (u.isSystemAdmin() && countSystemAdmins() <= 1) {
            throw new BadRequestException("Tiene que quedar al menos un Superadmin.");
        }
        users.delete(u);
    }

    private long countSystemAdmins() {
        return users.countByRoleSystemTrue();
    }

    /**
     * Sólo un superadmin puede asignar un rol "system" (Superadmin) a un
     * usuario — sin esto, un admin normal con permiso USERS_MANAGE podría
     * auto-otorgarse el rol Superadmin llamando directo a la API.
     */
    private void assertCanAssign(Role role, String actingDni) {
        if (!role.isSystem()) return;
        AdminUser acting = users.findByDniForTenant(actingDni, TenantContext.getTenantId()).orElse(null);
        if (acting == null || !acting.isSystemAdmin()) {
            throw new BadRequestException("Sólo un superadmin puede asignar el rol Superadmin.");
        }
    }
}
