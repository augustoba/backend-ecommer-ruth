package com.estilospequenos.controller;

import com.estilospequenos.dto.RoleDtos.PermissionInfo;
import com.estilospequenos.dto.RoleDtos.RoleRequest;
import com.estilospequenos.dto.RoleDtos.RoleResponse;
import com.estilospequenos.model.Permission;
import com.estilospequenos.model.Role;
import com.estilospequenos.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('USERS_MANAGE')")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping("/permissions")
    public List<PermissionInfo> permissions() {
        return Arrays.stream(Permission.values()).map(PermissionInfo::of).toList();
    }

    @GetMapping("/roles")
    public List<RoleResponse> list() {
        return service.findAll().stream()
                .map(r -> RoleResponse.from(r, service.userCount(r.getId())))
                .toList();
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest req) {
        Role r = service.create(req);
        return ResponseEntity.status(201).body(RoleResponse.from(r, 0));
    }

    @PutMapping("/roles/{id}")
    public RoleResponse update(@PathVariable String id, @Valid @RequestBody RoleRequest req) {
        Role r = service.update(id, req);
        return RoleResponse.from(r, service.userCount(r.getId()));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
