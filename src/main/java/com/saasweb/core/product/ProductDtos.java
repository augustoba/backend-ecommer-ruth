package com.saasweb.core.product;

import com.saasweb.core.product.Product;
import com.saasweb.core.product.ProductParam;
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
            /** Específico de indumentaria — opcional, otros rubros no lo usan. */
            String ageRange,
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
            /** null = 21% (default). Sólo importa para Factura A/B de ARCA. */
            BigDecimal ivaRate,
            /** Umbral de stock bajo propio del producto (unidades por talle). null = default global. */
            Integer lowStockThreshold,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks,
            /** Código de barras real (EAN/UPC) — opcional. */
            @Size(max = 64) String barcode
    ) {}

    public record StockPatch(@NotBlank String size, int stock, String note) {}

    public record PurchaseRequest(
            @NotBlank String size,
            int quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitCost,
            String supplierId
    ) {}

    public record StockMovementResponse(
            String id, String productId, String productName, String size, int quantityDelta,
            StockMovementReason reason, String note, String referenceId, BigDecimal unitCost,
            String createdByName, Instant createdAt
    ) {
        public static StockMovementResponse from(StockMovement m) {
            return new StockMovementResponse(m.getId(), m.getProductId(), m.getProductName(), m.getSize(),
                    m.getQuantityDelta(), m.getReason(), m.getNote(), m.getReferenceId(), m.getUnitCost(),
                    m.getCreatedByName(), m.getCreatedAt());
        }
    }

    public record ActivePatch(@NotNull Boolean active) {}

    public record DiscontinuedPatch(@NotNull Boolean discontinued) {}

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
            /** null = 21% (default). Sólo importa para Factura A/B de ARCA. */
            BigDecimal ivaRate,
            Integer lowStockThreshold,
            Map<String, List<String>> params,
            List<SizeStockDto> sizeStocks,
            String barcode
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
                    p.getImageUrl(), List.copyOf(p.getImages()), p.getVideoUrl(), p.isActive(), p.isDiscontinued(),
                    p.isDeleted(), p.getCreatedAt(), p.getSizeScaleId(), p.getSupplierId(), p.getCostPrice(),
                    p.getIvaRate(), p.getLowStockThreshold(), params, stocks, p.getBarcode());
        }
    }
}
