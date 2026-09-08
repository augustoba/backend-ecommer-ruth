package com.estilospequenos.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Genera y valida los JWT del panel de administración (HS256). */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(AppProperties props) {
        byte[] secret = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 caracteres (define JWT_SECRET).");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationMinutes = props.getJwt().getExpirationMinutes();
    }

    public TokenData generate(String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);
        String token = Jwts.builder()
                .subject(username)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new TokenData(token, exp);
    }

    /** @return el username (subject) si el token es válido; null si no. */
    public String validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public record TokenData(String token, Instant expiresAt) {}
}
