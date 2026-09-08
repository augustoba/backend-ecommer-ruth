package com.estilospequenos.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String ageRange;

    /** Puede ser una URL o un data URI (imagen embebida) — por eso el largo. */
    @Column(nullable = false, length = 5_000_000)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Escala de talle elegida (id de SizeScale). */
    private String sizeScaleId;

    /** Proveedor (id de Supplier) — info interna del admin. */
    private String supplierId;

    /** Precio de compra al proveedor — info interna del admin. */
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    @ElementCollection
    @CollectionTable(name = "product_size_stock", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "idx")
    private List<SizeStock> sizeStocks = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_param", joinColumns = @JoinColumn(name = "product_id"))
    private Set<ProductParam> params = new LinkedHashSet<>();
}
