package com.estilospequenos.controller;

import com.estilospequenos.dto.AccountDtos.AccountResponse;
import com.estilospequenos.dto.AccountDtos.ChangePasswordRequest;
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
        return new AccountResponse(user.getNombre(), user.getApellido(), user.getDni(), user.getEmail());
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication auth,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(auth.getName(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
