package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Grupo de parametría (ej: "Público", "Tipo de prenda", "Estación"). */
@Entity
@Table(name = "param_group")
@Getter
@Setter
@NoArgsConstructor
public class ParamGroup {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    /** true = un producto puede tener varias opciones de este grupo (ej: Estación) */
    @Column(nullable = false)
    private boolean multiple;

    /** true = aparece como filtro en el catálogo público */
    @Column(nullable = false)
    private boolean showInCatalog = true;

    /** true = grupo de sistema: no se puede eliminar */
    @Column(nullable = false)
    private boolean system;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ParamOption> options = new ArrayList<>();

    public void addOption(ParamOption option) {
        option.setGroup(this);
        option.setPosition(options.size());
        options.add(option);
    }
}
