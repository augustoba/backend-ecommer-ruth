package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Config global de descuentos (una sola fila, id fijo). */
@Entity
@Table(name = "discount_config")
@Getter
@Setter
@NoArgsConstructor
public class DiscountConfig {

    public enum CombineMode { MEJOR, COMBINAR }

    public static final String SINGLETON_ID = "config";

    @Id
    private String id = SINGLETON_ID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CombineMode combineMode = CombineMode.MEJOR;
}
