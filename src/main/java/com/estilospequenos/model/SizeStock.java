package com.estilospequenos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Stock de un talle puntual de un producto. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SizeStock {

    @Column(name = "size_value", nullable = false)
    private String size;

    @Column(nullable = false)
    private int stock;
}
