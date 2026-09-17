package com.saasweb.core.auth;

import com.saasweb.config.JwtService;
import com.saasweb.core.auth.AccountDtos.ForgotPasswordRequest;
import com.saasweb.core.auth.AccountDtos.ResetPasswordRequest;
import com.saasweb.core.admin.AdminUserDtos.MeResponse;
import com.saasweb.core.auth.LoginRequest;
import com.saasweb.core.auth.TokenResponse;
import com.saasweb.core.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Login del panel de administración. Devuelve el JWT a mandar como Bearer. */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        JwtService.TokenData data = authService.login(req.dni(), req.password(), clientIp(http));
        return ResponseEntity.ok(TokenResponse.bearer(data.token(), data.expiresAt()));
    }

    /**
     * "Olvidé mi contraseña": manda un link de un solo uso por mail para
     * elegir una contraseña nueva. Siempre responde igual, exista o no ese
     * DNI (no revela nada).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req, HttpServletRequest http) {
        authService.forgotPassword(req.dni(), clientIp(http));
        return ResponseEntity.noContent().build();
    }

    /** Confirma el link de "olvidé mi contraseña" con la contraseña nueva. Público. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    /** Quién soy y qué permisos tengo (para que el panel muestre/oculte cosas). */
    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        return MeResponse.from(authService.get(auth.getName()));
    }

    /** IP del cliente, respetando el primer hop de {@code X-Forwarded-For} si viene por proxy. */
    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
