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
            /** Link a un video de la prenda (YouTube). Opcional. */
            String videoUrl,
            Boolean active,
            /** true = no se repone más (deja de aparecer en "por reponer"). */
            Boolean discontinued,
            String sizeScaleId,
            String supplierId,
            BigDecimal costPrice,
            /** Umbral de stock bajo propio del producto (unidades por talle). null = default global. */
            Integer lowStockThreshold,
            /** Código de barras interno u original del fabricante. Opcional. */
            @Size(max = 64) String barcode,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks
    ) {}

    public record StockPatch(@NotBlank String size, int stock, String note) {}

    public record ActivePatch(@NotNull Boolean active) {}

    public record DiscontinuedPatch(@NotNull Boolean discontinued) {}

    /** Admin (panel): incluye todo, también `costPrice`/`supplierId` (info interna, ver PROYECTO.md §5). */
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
            /** Link a un video de la prenda (YouTube). null si no tiene. */
            String videoUrl,
            boolean active,
            boolean discontinued,
            boolean deleted,
            Instant createdAt,
            String sizeScaleId,
            String supplierId,
            BigDecimal costPrice,
            Integer lowStockThreshold,
            String barcode,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                    p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getAgeRange(),
                    p.getImageUrl(), List.copyOf(p.getImages()), p.getVideoUrl(), p.isActive(), p.isDiscontinued(),
                    p.isDeleted(), p.getCreatedAt(), p.getSizeScaleId(), p.getSupplierId(), p.getCostPrice(),
                    p.getLowStockThreshold(), p.getBarcode(), paramsOf(p), stocksOf(p));
        }
    }

    /**
     * Detalle público (`GET /api/products/{id}`): sin `costPrice`/`supplierId`
     * (info interna del admin, no se expone en el catálogo — PROYECTO.md §5),
     * pero con la galería completa (`images[]`), que la ficha de producto sí
     * muestra.
     */
    public record PublicProductResponse(
            String id,
            String name,
            String description,
            BigDecimal price,
            String ageRange,
            String imageUrl,
            List<String> images,
            String videoUrl,
            boolean active,
            boolean discontinued,
            Instant createdAt,
            String sizeScaleId,
            Integer lowStockThreshold,
            String barcode,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks
    ) {
        public static PublicProductResponse from(Product p) {
            return new PublicProductResponse(
                    p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getAgeRange(),
                    p.getImageUrl(), List.copyOf(p.getImages()), p.getVideoUrl(), p.isActive(), p.isDiscontinued(),
                    p.getCreatedAt(), p.getSizeScaleId(), p.getLowStockThreshold(), p.getBarcode(),
                    paramsOf(p), stocksOf(p));
        }
    }

    /**
     * Listado público (`GET /api/products`, `GET /api/products/best-sellers`):
     * además de omitir `costPrice`/`supplierId`, tampoco trae `images[]`
     * completo — sólo `imageUrl` (la portada), que es lo único que usan las
     * tarjetas del catálogo (`product-card.component.html` sólo lee
     * `product().imageUrl`). El array completo pesaba ~42% del payload de este
     * endpoint sin usarse.
     */
    public record PublicProductListResponse(
            String id,
            String name,
            String description,
            BigDecimal price,
            String ageRange,
            String imageUrl,
            String videoUrl,
            boolean active,
            boolean discontinued,
            Instant createdAt,
            String sizeScaleId,
            Integer lowStockThreshold,
            String barcode,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks
    ) {
        /** `coverImageUrl` se resuelve aparte (en lote) para no disparar el `@ElementCollection` lazy de `images`. */
        public static PublicProductListResponse from(Product p, String coverImageUrl) {
            return new PublicProductListResponse(
                    p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getAgeRange(),
                    coverImageUrl, p.getVideoUrl(), p.isActive(), p.isDiscontinued(), p.getCreatedAt(),
                    p.getSizeScaleId(), p.getLowStockThreshold(), p.getBarcode(), paramsOf(p), stocksOf(p));
        }
    }

    private static Map<String, List<String>> paramsOf(Product p) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        for (ProductParam pp : p.getParams()) {
            params.computeIfAbsent(pp.getGroupId(), k -> new ArrayList<>()).add(pp.getOptionId());
        }
        return params;
    }

    private static List<SizeStockDto> stocksOf(Product p) {
        return p.getSizeStocks().stream()
                .map(s -> new SizeStockDto(s.getSize(), s.getStock()))
                .toList();
    }
}
