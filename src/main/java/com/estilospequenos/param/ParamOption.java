package com.estilospequenos.param;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Opción dentro de un ParamGroup (ej: "Bebé", "Remera", "Verano"). */
@Entity
@Table(name = "param_option")
@Getter
@Setter
@NoArgsConstructor
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
}
