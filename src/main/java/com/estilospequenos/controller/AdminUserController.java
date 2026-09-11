package com.estilospequenos.controller;

import com.estilospequenos.dto.AdminUserDtos.CreateUserRequest;
import com.estilospequenos.dto.AdminUserDtos.UpdateUserRequest;
import com.estilospequenos.dto.AdminUserDtos.UserResponse;
import com.estilospequenos.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('USERS_MANAGE')")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req, Authentication auth) {
        return ResponseEntity.status(201).body(UserResponse.from(service.create(req, auth.getName())));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest req,
                               Authentication auth) {
        return UserResponse.from(service.update(id, req, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        service.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
