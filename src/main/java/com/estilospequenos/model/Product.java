package com.estilospequenos.model;

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

    /**
     * Fotos del producto, en orden. La primera es la portada (la que se ve en
     * las tarjetas del catálogo, el carrito y el listado del panel). Cada valor
     * puede ser una URL o un data URI (imagen embebida) — por eso el largo.
     */
    @ElementCollection
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "idx")
    @Column(name = "url", nullable = false, length = 5_000_000)
    private List<String> images = new ArrayList<>();

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

    /**
     * A partir de cuántas unidades por talle este producto se considera "stock
     * bajo" (para las alertas de reposición). null = usar el default global (3).
     */
    private Integer lowStockThreshold;

    @ElementCollection
    @CollectionTable(name = "product_size_stock", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "idx")
    private List<SizeStock> sizeStocks = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_param", joinColumns = @JoinColumn(name = "product_id"))
    private Set<ProductParam> params = new LinkedHashSet<>();

    /** Portada: la primera foto, o null si todavía no tiene ninguna. */
    @Transient
    public String getImageUrl() {
        return images.isEmpty() ? null : images.get(0);
    }
}
