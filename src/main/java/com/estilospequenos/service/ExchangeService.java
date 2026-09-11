package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.ExchangeDtos.CreateExchangeRequest;
import com.estilospequenos.dto.ExchangeDtos.ExchangeItem;
import com.estilospequenos.model.Exchange;
import com.estilospequenos.model.ExchangeLine;
import com.estilospequenos.model.Product;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.ExchangeRepository;
import com.estilospequenos.repository.ProductRepository;
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
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Exchange get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Cambio", id));
    }

    public Exchange create(CreateExchangeRequest req, String processedByDni) {
        Exchange ex = new Exchange();
        ex.setId(UUID.randomUUID().toString());
        ex.setNumber(repo.maxNumber() + 1);
        ex.setCustomerName(req.customerName() == null || req.customerName().isBlank()
                ? "Cambio en el local" : req.customerName().trim());
        ex.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        ex.setProcessedByDni(processedByDni);
        ex.setProcessedByName(processedByDni == null || processedByDni.isBlank() ? null
                : adminUsers.findByDni(processedByDni).map(u -> u.getNombre() + " " + u.getApellido()).orElse(null));

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
            productService.incrementStock(p.getId(), item.size(), item.quantity());
        }

        BigDecimal takenTotal = BigDecimal.ZERO;
        for (ExchangeItem item : req.taken()) {
            Product p = product(item.productId());
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            takenTotal = takenTotal.add(line);
            ex.addLine(line(ExchangeLine.Kind.LLEVADA, p, item));
            productService.decrementStock(p.getId(), item.size(), item.quantity());
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
        return productRepo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
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
