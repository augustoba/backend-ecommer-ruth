package com.estilospequenos.controller;

import com.estilospequenos.dto.AccountDtos.AccountResponse;
import com.estilospequenos.dto.AccountDtos.ChangePasswordRequest;
import com.estilospequenos.dto.AccountDtos.ChangeRecoveryRequest;
import com.estilospequenos.model.AdminUser;
import com.estilospequenos.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Gestión de la cuenta del admin logueado (`/api/admin/account`). */
@RestController
@RequestMapping("/api/admin/account")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public AccountResponse me(Authentication auth) {
        AdminUser user = authService.get(auth.getName());
        return new AccountResponse(user.getUsername(), user.getRecoveryHash() != null);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication auth,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(auth.getName(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/recovery")
    public ResponseEntity<Void> changeRecovery(Authentication auth,
                                               @Valid @RequestBody ChangeRecoveryRequest req) {
        authService.changeRecoveryPhrase(auth.getName(), req.currentPassword(), req.recoveryPhrase());
        return ResponseEntity.noContent().build();
    }
}
