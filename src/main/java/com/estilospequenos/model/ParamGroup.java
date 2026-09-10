package com.estilospequenos.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Grupo de parametría (ej: "Público", "Tipo de prenda", "Estación"). */
@Entity
@Table(name = "param_group")
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

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isShowInCatalog() {
        return showInCatalog;
    }

    public void setShowInCatalog(boolean showInCatalog) {
        this.showInCatalog = showInCatalog;
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

    public List<ParamOption> getOptions() {
        return options;
    }

    public void setOptions(List<ParamOption> options) {
        this.options = options;
    }
}
