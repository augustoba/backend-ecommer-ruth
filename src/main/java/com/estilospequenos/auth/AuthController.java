package com.estilospequenos.auth;

import com.estilospequenos.auth.dto.LoginRequest;
import com.estilospequenos.auth.dto.TokenResponse;
import com.estilospequenos.config.AppProperties;
import com.estilospequenos.config.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppProperties props;
    private final JwtService jwtService;

    public AuthController(AppProperties props, JwtService jwtService) {
        this.props = props;
        this.jwtService = jwtService;
    }

    /** Login del panel de administración. Devuelve el JWT a mandar como Bearer. */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        boolean ok = props.getAdmin().getUsername().equals(req.username().trim())
                && props.getAdmin().getPassword().equals(req.password());
        if (!ok) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }
        JwtService.TokenData data = jwtService.generate(props.getAdmin().getUsername());
        return ResponseEntity.ok(TokenResponse.bearer(data.token(), data.expiresAt()));
    }
}
