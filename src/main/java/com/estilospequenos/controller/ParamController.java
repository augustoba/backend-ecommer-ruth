package com.estilospequenos.controller;

import com.estilospequenos.dto.ParamDtos.GroupRequest;
import com.estilospequenos.dto.ParamDtos.GroupResponse;
import com.estilospequenos.dto.ParamDtos.OptionRequest;
import com.estilospequenos.service.ParamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ParamController {

    private final ParamService service;

    public ParamController(ParamService service) {
        this.service = service;
    }

    /** Público: parametrías para los filtros del catálogo. */
    @GetMapping("/api/param-groups")
    public List<GroupResponse> publicList() {
        return service.findAll().stream().map(GroupResponse::from).toList();
    }

    // --- Admin (mutaciones: requieren PARAMS_MANAGE) ---

    @GetMapping("/api/admin/param-groups")
    public List<GroupResponse> list() {
        return service.findAll().stream().map(GroupResponse::from).toList();
    }

    @PostMapping("/api/admin/param-groups")
    @PreAuthorize("hasAuthority('PARAMS_MANAGE')")
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest req) {
        return ResponseEntity.status(201).body(GroupResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/param-groups/{id}")
    @PreAuthorize("hasAuthority('PARAMS_MANAGE')")
    public GroupResponse update(@PathVariable String id, @Valid @RequestBody GroupRequest req) {
        return GroupResponse.from(service.update(id, req));
    }

    @DeleteMapping("/api/admin/param-groups/{id}")
    @PreAuthorize("hasAuthority('PARAMS_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/param-groups/{id}/options")
    @PreAuthorize("hasAuthority('PARAMS_MANAGE')")
    public ResponseEntity<GroupResponse> addOption(@PathVariable String id, @Valid @RequestBody OptionRequest req) {
        return ResponseEntity.status(201).body(GroupResponse.from(service.addOption(id, req)));
    }

    @PutMapping("/api/admin/param-groups/{id}/options/{optionId}")
    @PreAuthorize("hasAuthority('PARAMS_MANAGE')")
    public GroupResponse updateOption(@PathVariable String id, @PathVariable String optionId,
                                      @Valid @RequestBody OptionRequest req) {
        return GroupResponse.from(service.updateOption(id, optionId, req));
    }

    @DeleteMapping("/api/admin/param-groups/{id}/options/{optionId}")
    @PreAuthorize("hasAuthority('PARAMS_MANAGE')")
    public GroupResponse removeOption(@PathVariable String id, @PathVariable String optionId) {
        return GroupResponse.from(service.removeOption(id, optionId));
    }
}
