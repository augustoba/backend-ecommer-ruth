package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.AdminUserDtos.CreateUserRequest;
import com.estilospequenos.dto.AdminUserDtos.UpdateUserRequest;
import com.estilospequenos.model.AdminUser;
import com.estilospequenos.model.Role;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.RoleRepository;
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
        return users.findAllByOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public AdminUser get(String id) {
        return users.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    public AdminUser create(CreateUserRequest req) {
        String username = req.username().trim();
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("Ya existe un usuario con ese nombre.");
        }
        Role role = roles.findById(req.roleId())
                .orElseThrow(() -> new BadRequestException("El rol elegido no existe."));

        AdminUser u = new AdminUser();
        u.setId(UUID.randomUUID().toString());
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(role);
        u.setEnabled(req.enabled() == null || req.enabled());
        u.setFirstName(blankToNull(req.firstName()));
        u.setLastName(blankToNull(req.lastName()));
        u.setEmail(blankToNull(req.email()));
        return users.save(u);
    }

    public AdminUser update(String id, UpdateUserRequest req, String actingUsername) {
        AdminUser u = get(id);
        boolean editingSelf = u.getUsername().equalsIgnoreCase(actingUsername);

        if (req.roleId() != null && !req.roleId().isBlank()) {
            Role role = roles.findById(req.roleId())
                    .orElseThrow(() -> new BadRequestException("El rol elegido no existe."));
            if (editingSelf && u.isSystemAdmin() && !role.isSystem()) {
                throw new BadRequestException("No podés sacarte a vos mismo el rol de Administrador.");
            }
            u.setRole(role);
        }
        if (req.enabled() != null) {
            if (editingSelf && !req.enabled()) {
                throw new BadRequestException("No podés deshabilitar tu propio usuario.");
            }
            u.setEnabled(req.enabled());
        }
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        if (req.firstName() != null) u.setFirstName(blankToNull(req.firstName()));
        if (req.lastName() != null) u.setLastName(blankToNull(req.lastName()));
        if (req.email() != null) u.setEmail(blankToNull(req.email()));
        return users.save(u);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    public void delete(String id, String actingUsername) {
        AdminUser u = get(id);
        if (u.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new BadRequestException("No podés borrar tu propio usuario.");
        }
        if (u.isSystemAdmin() && countSystemAdmins() <= 1) {
            throw new BadRequestException("Tiene que quedar al menos un Administrador.");
        }
        users.delete(u);
    }

    private long countSystemAdmins() {
        return users.findAll().stream().filter(AdminUser::isSystemAdmin).count();
    }
}
