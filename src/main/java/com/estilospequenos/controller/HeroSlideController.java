package com.estilospequenos.controller;

import com.estilospequenos.dto.HeroSlideDtos.ReorderRequest;
import com.estilospequenos.dto.HeroSlideDtos.SlideRequest;
import com.estilospequenos.dto.HeroSlideDtos.SlideResponse;
import com.estilospequenos.service.HeroSlideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HeroSlideController {

    private final HeroSlideService service;

    public HeroSlideController(HeroSlideService service) {
        this.service = service;
    }

    @GetMapping("/api/hero-slides")
    public List<SlideResponse> publicList() {
        return service.findAll().stream().map(SlideResponse::from).toList();
    }

    @GetMapping("/api/admin/hero-slides")
    public List<SlideResponse> list() {
        return service.findAll().stream().map(SlideResponse::from).toList();
    }

    @PostMapping("/api/admin/hero-slides")
    public ResponseEntity<SlideResponse> create(@Valid @RequestBody SlideRequest req) {
        return ResponseEntity.status(201).body(SlideResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/hero-slides/{id}")
    public SlideResponse update(@PathVariable String id, @Valid @RequestBody SlideRequest req) {
        return SlideResponse.from(service.update(id, req));
    }

    @DeleteMapping("/api/admin/hero-slides/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/hero-slides/reorder")
    public List<SlideResponse> reorder(@Valid @RequestBody ReorderRequest req) {
        return service.reorder(req.ids()).stream().map(SlideResponse::from).toList();
    }
}
