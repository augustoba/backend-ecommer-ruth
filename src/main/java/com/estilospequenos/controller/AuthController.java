package com.estilospequenos.controller;

import com.estilospequenos.config.JwtService;
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
}
