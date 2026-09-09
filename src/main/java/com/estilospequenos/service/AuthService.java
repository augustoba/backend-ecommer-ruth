package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.config.AppProperties;
import com.estilospequenos.config.JwtService;
import com.estilospequenos.model.AdminUser;
import com.estilospequenos.model.Role;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.RoleRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final int MIN_PASSWORD = 4;

    private final AdminUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final LoginAttemptService loginAttempts;
    private final RoleRepository roles;

    public AuthService(AdminUserRepository users, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AppProperties props,
                       LoginAttemptService loginAttempts, RoleRepository roles) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
        this.loginAttempts = loginAttempts;
        this.roles = roles;
    }

    /**
     * Valida usuario/contraseña contra la tabla admin_user y devuelve un JWT.
     * {@code clientIp} se usa para el rate-limiting (ver LoginAttemptService).
     */
    public JwtService.TokenData login(String username, String rawPassword, String clientIp) {
        loginAttempts.assertNotBlocked(clientIp, username);
        AdminUser user = users.findByUsername(username == null ? "" : username.trim())
                .filter(AdminUser::isEnabled)
                .orElse(null);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginAttempts.recordFailure(clientIp, username);
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }
        loginAttempts.recordSuccess(clientIp, username);
        return jwtService.generate(user.getUsername());
    }

    /**
     * Recupera la cuenta: si la frase de recuperación es correcta, setea una
     * contraseña nueva y devuelve un JWT (queda logueado). Público.
     */
    public JwtService.TokenData recover(String username, String recoveryPhrase, String newPassword, String clientIp) {
        loginAttempts.assertNotBlocked(clientIp, username);
        AdminUser user = users.findByUsername(username == null ? "" : username.trim())
                .filter(AdminUser::isEnabled)
                .orElse(null);
        if (user == null || user.getRecoveryHash() == null
                || !passwordEncoder.matches(recoveryPhrase, user.getRecoveryHash())) {
            loginAttempts.recordFailure(clientIp, username);
            throw new BadCredentialsException("La frase de recuperación no coincide.");
        }
        loginAttempts.recordSuccess(clientIp, username);
        setPassword(user, newPassword);
        return jwtService.generate(user.getUsername());
    }

    /** Cambia la contraseña (requiere la actual). */
    public void changePassword(String username, String currentPassword, String newPassword) {
        AdminUser user = enabledByUsername(username);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta.");
        }
        setPassword(user, newPassword);
    }

    /** Cambia la frase de recuperación (requiere la contraseña actual). */
    public void changeRecoveryPhrase(String username, String currentPassword, String newPhrase) {
        AdminUser user = enabledByUsername(username);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta.");
        }
        if (newPhrase == null || newPhrase.trim().length() < MIN_PASSWORD) {
            throw new BadRequestException("La frase de recuperación es muy corta.");
        }
        user.setRecoveryHash(passwordEncoder.encode(newPhrase.trim()));
        users.save(user);
    }

    @Transactional(readOnly = true)
    public AdminUser get(String username) {
        return users.findByUsername(username.trim())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", username));
    }

    /**
     * Crea el admin inicial si no existe (password + frase de recuperación desde
     * `app.admin.*`, hasheados). Si ya existe pero le falta la frase de
     * recuperación, se la completa. Se llama desde el DataSeeder.
     */
    public void ensureInitialAdmin() {
        Role systemRole = roles.findFirstBySystemTrue().orElse(null);
        String username = props.getAdmin().getUsername();
        AdminUser admin = users.findByUsername(username).orElse(null);
        if (admin == null) {
            admin = new AdminUser();
            admin.setId(UUID.randomUUID().toString());
            admin.setUsername(username);
            admin.setPasswordHash(passwordEncoder.encode(props.getAdmin().getPassword()));
            admin.setRecoveryHash(passwordEncoder.encode(props.getAdmin().getRecoveryPhrase()));
            admin.setEnabled(true);
            admin.setRole(systemRole);
            users.save(admin);
        } else {
            boolean dirty = false;
            if (admin.getRecoveryHash() == null) {
                admin.setRecoveryHash(passwordEncoder.encode(props.getAdmin().getRecoveryPhrase()));
                dirty = true;
            }
            // Backfill: usuarios viejos sin rol → Administrador.
            if (admin.getRole() == null && systemRole != null) {
                admin.setRole(systemRole);
                dirty = true;
            }
            if (dirty) users.save(admin);
        }
        // Cualquier otro usuario sin rol (datos viejos) también queda como Administrador.
        if (systemRole != null) {
            for (AdminUser u : users.findAll()) {
                if (u.getRole() == null) {
                    u.setRole(systemRole);
                    users.save(u);
                }
            }
        }
    }

    private AdminUser enabledByUsername(String username) {
        return users.findByUsername(username == null ? "" : username.trim())
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));
    }

    private void setPassword(AdminUser user, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD) {
            throw new BadRequestException("La contraseña nueva debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
    }
}
