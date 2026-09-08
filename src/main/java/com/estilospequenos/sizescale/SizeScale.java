package com.estilospequenos.sizescale;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Escala de talle (ej: "Ropa niños", "Calzado adultos"). */
@Entity
@Table(name = "size_scale")
@Getter
@Setter
@NoArgsConstructor
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
}
