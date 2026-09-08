package com.estilospequenos.dto;

import com.estilospequenos.model.Product;
import com.estilospequenos.model.ProductParam;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductDtos {

    private ProductDtos() {}

    public record SizeStockDto(@NotBlank String size, int stock) {}

    public record ProductRequest(
            @NotBlank @Size(min = 2) String name,
            @NotBlank @Size(min = 5) String description,
            @NotNull @DecimalMin("0.0") BigDecimal price,
            @NotBlank String ageRange,
            /** Fotos del producto, en orden. La primera es la portada. Al menos una. */
            @NotEmpty List<@NotBlank String> images,
            Boolean active,
            String sizeScaleId,
            String supplierId,
            BigDecimal costPrice,
            /** Umbral de stock bajo propio del producto (unidades por talle). null = default global. */
            Integer lowStockThreshold,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks
    ) {}

    public record StockPatch(@NotBlank String size, int stock) {}

    public record ActivePatch(@NotNull Boolean active) {}

    public record ProductResponse(
            String id,
            String name,
            String description,
            BigDecimal price,
            String ageRange,
            /** Portada (primera foto), por compatibilidad con las tarjetas / el carrito. */
            String imageUrl,
            /** Todas las fotos, en orden. */
            List<String> images,
            boolean active,
            Instant createdAt,
            String sizeScaleId,
            String supplierId,
            BigDecimal costPrice,
            Integer lowStockThreshold,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks
    ) {
        public static ProductResponse from(Product p) {
            Map<String, List<String>> params = new LinkedHashMap<>();
            for (ProductParam pp : p.getParams()) {
                params.computeIfAbsent(pp.getGroupId(), k -> new ArrayList<>()).add(pp.getOptionId());
            }
            List<SizeStockDto> stocks = p.getSizeStocks().stream()
                    .map(s -> new SizeStockDto(s.getSize(), s.getStock()))
                    .toList();
            return new ProductResponse(
                    p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getAgeRange(),
                    p.getImageUrl(), List.copyOf(p.getImages()), p.isActive(), p.getCreatedAt(),
                    p.getSizeScaleId(), p.getSupplierId(), p.getCostPrice(), p.getLowStockThreshold(),
                    params, stocks);
        }
    }
}
