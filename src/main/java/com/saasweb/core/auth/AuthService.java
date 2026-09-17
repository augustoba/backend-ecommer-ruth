package com.saasweb.core.auth;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.config.AppProperties;
import com.saasweb.config.JwtService;
import com.saasweb.core.admin.AdminUser;
import com.saasweb.core.admin.Role;
import com.saasweb.core.admin.AdminUserRepository;
import com.saasweb.core.admin.RoleRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final int MIN_PASSWORD = 4;
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private final SecureRandom random = new SecureRandom();

    private final AdminUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final LoginAttemptService loginAttempts;
    private final RoleRepository roles;
    private final AccountMailService accountMailService;

    public AuthService(AdminUserRepository users, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AppProperties props,
                       LoginAttemptService loginAttempts, RoleRepository roles,
                       AccountMailService accountMailService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
        this.loginAttempts = loginAttempts;
        this.roles = roles;
        this.accountMailService = accountMailService;
    }

    /**
     * Valida DNI/contraseña contra la tabla admin_user y devuelve un JWT.
     * {@code clientIp} se usa para el rate-limiting (ver LoginAttemptService).
     */
    public JwtService.TokenData login(String dni, String rawPassword, String clientIp) {
        loginAttempts.assertNotBlocked(clientIp, dni);
        AdminUser user = users.findByDniForTenant(dni == null ? "" : dni.trim(), TenantContext.getTenantId())
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
     * Recupera la cuenta: genera un token de un solo uso (vence a la hora) y
     * manda un LINK por mail para elegir una contraseña nueva — nunca una
     * contraseña en sí. Antes esto generaba y mandaba una contraseña al azar
     * directo por mail, lo que además de viajar en texto plano tenía un
     * problema más serio: como el DNI no es realmente secreto, cualquiera
     * que lo supiera podía invalidar la contraseña real de otro admin en
     * cualquier momento con sólo pedir la recuperación (no hacía falta leer
     * el mail ajeno para causar el daño). Con el link, pedir la recuperación
     * ya no cambia nada de la cuenta hasta que alguien con acceso al mail
     * efectivamente lo abre y confirma una contraseña nueva.
     * Público. No revela si el DNI existe o no (siempre responde igual).
     */
    public void forgotPassword(String dni, String clientIp) {
        loginAttempts.assertNotBlocked(clientIp, dni);
        AdminUser user = users.findByDniForTenant(dni == null ? "" : dni.trim(), TenantContext.getTenantId())
                .filter(AdminUser::isEnabled)
                .orElse(null);
        if (user == null) {
            loginAttempts.recordFailure(clientIp, dni);
            return;
        }
        loginAttempts.recordSuccess(clientIp, dni);
        String rawToken = generateResetToken();
        user.setResetTokenHash(hashToken(rawToken));
        user.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
        users.save(user);
        String link = props.getUrls().getFrontend() + "/admin/restablecer-clave?token=" + rawToken;
        accountMailService.sendPasswordResetLink(user.getEmail(), user.getNombre(), link);
    }

    /**
     * Confirma la recuperación: valida el token (existe, no venció) y deja
     * la contraseña nueva. De un solo uso — el token se borra apenas se usa,
     * sea cual sea el resultado, para que no quede reutilizable ni siquiera
     * si algo falla después.
     */
    public void resetPassword(String token, String newPassword) {
        String tokenHash = hashToken(token == null ? "" : token.trim());
        AdminUser user = users.findByResetTokenHashForTenant(tokenHash, TenantContext.getTenantId())
                .orElseThrow(() -> new BadRequestException("El link venció o ya se usó. Pedí uno nuevo."));
        Instant expiresAt = user.getResetTokenExpiresAt();
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            users.save(user);
            throw new BadRequestException("El link venció o ya se usó. Pedí uno nuevo.");
        }
        setPassword(user, newPassword);
    }

    /** Cambia la contraseña (requiere la actual). */
    public void changePassword(String dni, String currentPassword, String newPassword) {
        AdminUser user = enabledByDni(dni);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta.");
        }
        setPassword(user, newPassword);
    }

    /** Token de un solo uso, URL-safe, 32 bytes de entropía (no adivinable). */
    private String generateResetToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Sólo se guarda el hash del token (nunca el token en sí) — mismo
     * criterio que la contraseña, por si alguna vez se filtra la base.
     */
    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    @Transactional(readOnly = true)
    public AdminUser get(String dni) {
        return users.findByDniForTenant(dni.trim(), TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", dni));
    }

    /**
     * Crea la cuenta inicial de la dueña de la tienda si no existe (password
     * desde `app.admin.*`, hasheada), con rol "Administrador" (normal, no
     * system). Se llama desde el DataSeeder, una vez por tenant.
     */
    public void ensureInitialAdmin(String tenantId) {
        Role adminRole = roles.findByTenantIdAndNameIgnoreCase(tenantId, "Administrador").orElse(null);
        String dni = props.getAdmin().getDni();
        if (users.findByDniAndTenantId(dni, tenantId).isPresent()) return;
        AdminUser admin = new AdminUser();
        admin.setId(UUID.randomUUID().toString());
        admin.setTenantId(tenantId);
        admin.setDni(dni);
        admin.setNombre(props.getAdmin().getNombre());
        admin.setApellido(props.getAdmin().getApellido());
        admin.setEmail(props.getAdmin().getEmail());
        admin.setPasswordHash(passwordEncoder.encode(props.getAdmin().getPassword()));
        admin.setEnabled(true);
        admin.setRole(adminRole);
        users.save(admin);
    }

    /**
     * Crea la cuenta inicial del superadmin (dueño de la plataforma) si no
     * existe, con el rol system (todos los permisos siempre) y el flag
     * {@code superAdmin=true} (acceso aparte a Cloudinary/mail, ver
     * {@link AdminUser#isSuperAdmin()}). Se llama desde el DataSeeder, después
     * de {@link #ensureInitialAdmin()}. Si la cuenta ya existe, sólo se asegura
     * de que tenga el flag y el rol seteados (no le toca la contraseña, para no
     * pisar un cambio hecho desde el panel).
     */
    public void ensureInitialSuperadmin() {
        Role superadminRole = roles.findFirstBySystemTrue().orElse(null);
        String dni = props.getSuperadmin().getDni();
        AdminUser superadmin = users.findByDniAndTenantIdIsNull(dni).orElse(null);
        if (superadmin == null) {
            superadmin = new AdminUser();
            superadmin.setId(UUID.randomUUID().toString());
            superadmin.setTenantId(null);
            superadmin.setDni(dni);
            superadmin.setNombre(props.getSuperadmin().getNombre());
            superadmin.setApellido(props.getSuperadmin().getApellido());
            superadmin.setEmail(props.getSuperadmin().getEmail());
            superadmin.setPasswordHash(passwordEncoder.encode(props.getSuperadmin().getPassword()));
            superadmin.setEnabled(true);
            superadmin.setRole(superadminRole);
            superadmin.setSuperAdmin(true);
            users.save(superadmin);
            return;
        }
        boolean dirty = false;
        if (!superadmin.isSuperAdmin()) {
            superadmin.setSuperAdmin(true);
            dirty = true;
        }
        if (superadmin.getRole() == null && superadminRole != null) {
            superadmin.setRole(superadminRole);
            dirty = true;
        }
        if (dirty) users.save(superadmin);
    }

    private AdminUser enabledByDni(String dni) {
        return users.findByDniForTenant(dni == null ? "" : dni.trim(), TenantContext.getTenantId())
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
