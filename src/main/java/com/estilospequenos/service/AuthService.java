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
     * Valida DNI/contraseña contra la tabla admin_user y devuelve un JWT.
     * {@code clientIp} se usa para el rate-limiting (ver LoginAttemptService).
     */
    public JwtService.TokenData login(String dni, String rawPassword, String clientIp) {
        loginAttempts.assertNotBlocked(clientIp, dni);
        AdminUser user = users.findByDni(dni == null ? "" : dni.trim())
                .filter(AdminUser::isEnabled)
                .orElse(null);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginAttempts.recordFailure(clientIp, dni);
            throw new BadCredentialsException("DNI o contraseña incorrectos");
        }
        loginAttempts.recordSuccess(clientIp, dni);
        return jwtService.generate(user.getDni());
    }

    /**
     * Recupera la cuenta: si la frase de recuperación es correcta, setea una
     * contraseña nueva y devuelve un JWT (queda logueado). Público.
     */
    public JwtService.TokenData recover(String dni, String recoveryPhrase, String newPassword, String clientIp) {
        loginAttempts.assertNotBlocked(clientIp, dni);
        AdminUser user = users.findByDni(dni == null ? "" : dni.trim())
                .filter(AdminUser::isEnabled)
                .orElse(null);
        if (user == null || user.getRecoveryHash() == null
                || !passwordEncoder.matches(recoveryPhrase, user.getRecoveryHash())) {
            loginAttempts.recordFailure(clientIp, dni);
            throw new BadCredentialsException("La frase de recuperación no coincide.");
        }
        loginAttempts.recordSuccess(clientIp, dni);
        setPassword(user, newPassword);
        return jwtService.generate(user.getDni());
    }

    /** Cambia la contraseña (requiere la actual). */
    public void changePassword(String dni, String currentPassword, String newPassword) {
        AdminUser user = enabledByDni(dni);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta.");
        }
        setPassword(user, newPassword);
    }

    /** Cambia la frase de recuperación (requiere la contraseña actual). */
    public void changeRecoveryPhrase(String dni, String currentPassword, String newPhrase) {
        AdminUser user = enabledByDni(dni);
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
    public AdminUser get(String dni) {
        return users.findByDni(dni.trim())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", dni));
    }

    /**
     * Crea la cuenta inicial de la dueña de la tienda si no existe (password +
     * frase de recuperación desde `app.admin.*`, hasheados), con rol
     * "Administrador" (normal, no system). Se llama desde el DataSeeder.
     */
    public void ensureInitialAdmin() {
        Role adminRole = roles.findByNameIgnoreCase("Administrador").orElse(null);
        String dni = props.getAdmin().getDni();
        AdminUser admin = users.findByDni(dni).orElse(null);
        if (admin == null) {
            admin = new AdminUser();
            admin.setId(UUID.randomUUID().toString());
            admin.setDni(dni);
            admin.setNombre(props.getAdmin().getNombre());
            admin.setApellido(props.getAdmin().getApellido());
            admin.setEmail(props.getAdmin().getEmail());
            admin.setPasswordHash(passwordEncoder.encode(props.getAdmin().getPassword()));
            admin.setRecoveryHash(passwordEncoder.encode(props.getAdmin().getRecoveryPhrase()));
            admin.setEnabled(true);
            admin.setRole(adminRole);
            users.save(admin);
        } else if (admin.getRecoveryHash() == null) {
            admin.setRecoveryHash(passwordEncoder.encode(props.getAdmin().getRecoveryPhrase()));
            users.save(admin);
        }
    }

    /**
     * Crea la cuenta inicial del superadmin (dueño de la plataforma) si no
     * existe, con el rol system (todos los permisos siempre). Se llama desde
     * el DataSeeder, después de {@link #ensureInitialAdmin()}.
     */
    public void ensureInitialSuperadmin() {
        Role superadminRole = roles.findFirstBySystemTrue().orElse(null);
        String dni = props.getSuperadmin().getDni();
        if (users.findByDni(dni).isPresent()) return;
        AdminUser superadmin = new AdminUser();
        superadmin.setId(UUID.randomUUID().toString());
        superadmin.setDni(dni);
        superadmin.setNombre(props.getSuperadmin().getNombre());
        superadmin.setApellido(props.getSuperadmin().getApellido());
        superadmin.setEmail(props.getSuperadmin().getEmail());
        superadmin.setPasswordHash(passwordEncoder.encode(props.getSuperadmin().getPassword()));
        superadmin.setEnabled(true);
        superadmin.setRole(superadminRole);
        users.save(superadmin);
    }

    private AdminUser enabledByDni(String dni) {
        return users.findByDni(dni == null ? "" : dni.trim())
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("DNI o contraseña incorrectos"));
    }

    private void setPassword(AdminUser user, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD) {
            throw new BadRequestException("La contraseña nueva debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
    }
}
