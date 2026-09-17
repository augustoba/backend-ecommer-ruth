package com.saasweb.core.exchange;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.exchange.ExchangeDtos.CreateExchangeRequest;
import com.saasweb.core.exchange.ExchangeDtos.ExchangeItem;
import com.saasweb.core.exchange.Exchange;
import com.saasweb.core.exchange.ExchangeLine;
import com.saasweb.core.product.Product;
import com.saasweb.core.admin.AdminUserRepository;
import com.saasweb.core.exchange.ExchangeRepository;
import com.saasweb.core.product.ProductRepository;
import com.saasweb.core.product.ProductService;
import com.saasweb.core.product.StockMovementReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cambios de prenda en el local. Al registrar un cambio: el stock de lo
 * devuelto vuelve a sumar y el de lo que se lleva se descuenta (estricto: si
 * falta stock, no se guarda nada). La diferencia de precio la calcula a precio
 * de lista actual.
 */
@Service
@Transactional
public class ExchangeService {

    private final ExchangeRepository repo;
    private final ProductRepository productRepo;
    private final ProductService productService;
    private final AdminUserRepository adminUsers;

    public ExchangeService(ExchangeRepository repo, ProductRepository productRepo,
                           ProductService productService, AdminUserRepository adminUsers) {
        this.repo = repo;
        this.productRepo = productRepo;
        this.productService = productService;
        this.adminUsers = adminUsers;
    }

    @Transactional(readOnly = true)
    public List<Exchange> findAll() {
        return repo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Exchange get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cambio", id));
    }

    public Exchange create(CreateExchangeRequest req, String processedByDni) {
        String tenantId = TenantContext.getTenantId();
        Exchange ex = new Exchange();
        ex.setId(UUID.randomUUID().toString());
        ex.setTenantId(tenantId);
        ex.setNumber(repo.maxNumber(tenantId) + 1);
        ex.setCustomerName(req.customerName() == null || req.customerName().isBlank()
                ? "Cambio en el local" : req.customerName().trim());
        ex.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        ex.setProcessedByDni(processedByDni);
        ex.setProcessedByName(processedByDni == null || processedByDni.isBlank() ? null
                : adminUsers.findByDniForTenant(processedByDni, tenantId).map(u -> u.getNombre() + " " + u.getApellido()).orElse(null));

        // Pre-chequeo de stock de lo que se lleva (agrupando por producto+talle).
        List<String> shortages = new ArrayList<>();
        for (ExchangeItem item : req.taken()) {
            Product p = product(item.productId());
            int available = productService.stockOf(p.getId(), item.size());
            if (item.quantity() > available) {
                shortages.add(p.getName() + " (talle " + item.size() + "): se lleva "
                        + item.quantity() + ", " + (available > 0 ? "quedan " + available : "sin stock"));
            }
        }
        if (!shortages.isEmpty()) {
            throw new BadRequestException("No hay stock para lo que se lleva: "
                    + String.join("; ", shortages) + ".");
        }

        BigDecimal returnedTotal = BigDecimal.ZERO;
        for (ExchangeItem item : req.returned()) {
            Product p = product(item.productId());
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            returnedTotal = returnedTotal.add(line);
            ex.addLine(line(ExchangeLine.Kind.DEVUELTA, p, item));
            productService.incrementStock(p.getId(), item.size(), item.quantity(),
                    StockMovementReason.CAMBIO_DEVUELTA, ex.getId(), null, processedByDni);
        }

        BigDecimal takenTotal = BigDecimal.ZERO;
        for (ExchangeItem item : req.taken()) {
            Product p = product(item.productId());
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            takenTotal = takenTotal.add(line);
            ExchangeLine takenLine = line(ExchangeLine.Kind.LLEVADA, p, item);
            // Costo congelado ACÁ (al procesar el cambio, momento real de la salida de stock).
            takenLine.setCostPrice(p.getCostPrice());
            ex.addLine(takenLine);
            productService.decrementStock(p.getId(), item.size(), item.quantity(),
                    StockMovementReason.CAMBIO_LLEVADA, ex.getId(), processedByDni);
        }

        BigDecimal difference = takenTotal.subtract(returnedTotal);
        ex.setReturnedTotal(returnedTotal);
        ex.setTakenTotal(takenTotal);
        ex.setDifference(difference);
        // El medio de pago sólo tiene sentido si el local cobra algo.
        ex.setPaymentMethod(difference.signum() > 0 ? req.paymentMethod() : null);

        return repo.save(ex);
    }

    private Product product(String id) {
        return productRepo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
    }

    private static ExchangeLine line(ExchangeLine.Kind kind, Product p, ExchangeItem item) {
        ExchangeLine l = new ExchangeLine();
        l.setId(UUID.randomUUID().toString());
        l.setKind(kind);
        l.setProductId(p.getId());
        l.setProductName(p.getName());
        l.setSize(item.size());
        l.setQuantity(item.quantity());
        l.setUnitPrice(p.getPrice());
        return l;
    }
}
