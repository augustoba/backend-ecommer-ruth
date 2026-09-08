package com.estilospequenos.supplier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Proveedor del local (info interna del admin). */
@Entity
@Table(name = "supplier")
@Getter
@Setter
@NoArgsConstructor
public class Supplier {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String address;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
