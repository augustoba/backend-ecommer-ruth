package com.estilospequenos.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Escala de talle (ej: "Ropa niños", "Calzado adultos"). */
@Entity
@Table(name = "size_scale")
public class SizeScale {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean system;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection
    @CollectionTable(name = "size_scale_value", joinColumns = @JoinColumn(name = "scale_id"))
    @OrderColumn(name = "idx")
    @Column(name = "size_value", nullable = false)
    private List<String> values = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
