package com.estilospequenos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/** Opción dentro de un ParamGroup (ej: "Bebé", "Remera", "Verano"). */
@Entity
@Table(name = "param_option")
public class ParamOption {

    @Id
    private String id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private ParamGroup group;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public ParamGroup getGroup() {
        return group;
    }

    public void setGroup(ParamGroup group) {
        this.group = group;
    }
}
