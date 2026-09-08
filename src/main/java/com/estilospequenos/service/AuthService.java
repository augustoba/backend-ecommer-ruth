package com.estilospequenos.service;

import com.estilospequenos.config.AppProperties;
import com.estilospequenos.config.JwtService;
import com.estilospequenos.model.AdminUser;
import com.estilospequenos.repository.AdminUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final AdminUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;

    public AuthService(AdminUserRepository users, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AppProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
    }

    /** Valida usuario/contraseña contra la tabla admin_user y devuelve un JWT. */
    public JwtService.TokenData login(String username, String rawPassword) {
        AdminUser user = users.findByUsername(username.trim())
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }
        return jwtService.generate(user.getUsername());
    }

    /**
     * Crea el admin inicial si todavía no hay ninguno, con las credenciales de
     * `app.admin.*` (contraseña hasheada). Se llama desde el DataSeeder.
     */
    public void ensureInitialAdmin() {
        if (users.count() > 0) return;
        AdminUser admin = new AdminUser();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername(props.getAdmin().getUsername());
        admin.setPasswordHash(passwordEncoder.encode(props.getAdmin().getPassword()));
        admin.setEnabled(true);
        users.save(admin);
    }
}
