package com.estilospequenos.controller;

import com.estilospequenos.config.JwtService;
import com.estilospequenos.dto.AccountDtos.RecoverRequest;
import com.estilospequenos.dto.LoginRequest;
import com.estilospequenos.dto.TokenResponse;
import com.estilospequenos.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        JwtService.TokenData data = authService.login(req.username(), req.password());
        return ResponseEntity.ok(TokenResponse.bearer(data.token(), data.expiresAt()));
    }

    /**
     * Recuperar la cuenta con la frase de recuperación. Setea la contraseña
     * nueva y devuelve un JWT (queda logueado).
     */
    @PostMapping("/recover")
    public ResponseEntity<TokenResponse> recover(@Valid @RequestBody RecoverRequest req) {
        JwtService.TokenData data =
                authService.recover(req.username(), req.recoveryPhrase(), req.newPassword());
        return ResponseEntity.ok(TokenResponse.bearer(data.token(), data.expiresAt()));
    }
}
