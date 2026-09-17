package com.saasweb.core.arca;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.admin.AdminUserRepository;
import com.saasweb.core.arca.ArcaInvoiceService.InvoiceResult;
import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderRepository;
import com.saasweb.core.order.OrderStatus;
import com.saasweb.core.order.SaleChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreditNoteService {

    private final CreditNoteRepository repo;
    private final OrderRepository orderRepo;
    private final ArcaInvoiceService arcaInvoiceService;
    private final AdminUserRepository adminUsers;

    public CreditNoteService(CreditNoteRepository repo, OrderRepository orderRepo,
                             ArcaInvoiceService arcaInvoiceService, AdminUserRepository adminUsers) {
        this.repo = repo;
        this.orderRepo = orderRepo;
        this.arcaInvoiceService = arcaInvoiceService;
        this.adminUsers = adminUsers;
    }

    @Transactional(readOnly = true)
    public List<CreditNote> findByOrder(String orderId) {
        return repo.findByTenantIdAndOrderIdOrderByCreatedAtDesc(TenantContext.getTenantId(), orderId);
    }

    /**
     * Emite una nota de crédito contra un pedido ya facturado con ARCA —
     * acción manual del panel (ítem 2), monto y motivo los decide quien la
     * pide. No toca stock ni el estado del pedido; siempre queda un registro
     * (apruebe o no ARCA), para poder ver el historial de intentos.
     */
    public CreditNote emit(String orderId, BigDecimal amount, String reason, String createdByDni) {
        String tenantId = TenantContext.getTenantId();
        Order order = orderRepo.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", orderId));
        if (order.getChannel() != SaleChannel.LOCAL || order.getStatus() != OrderStatus.PROCESADO) {
            throw new BadRequestException("Sólo se puede emitir una NC de una venta presencial ya cobrada.");
        }
        if (order.getInvoiceType() == null || !order.getInvoiceType().startsWith("FACTURA_")
                || order.getInvoiceCae() == null) {
            throw new BadRequestException("Este pedido no tiene una Factura ARCA aprobada.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("El monto tiene que ser mayor a 0.");
        }

        InvoiceResult result = arcaInvoiceService.emitirNotaCredito(tenantId, order.getInvoiceType(),
                order.getInvoicePuntoVenta(), order.getInvoiceNumber(), order.getInvoiceBuyerCuit(), amount);

        CreditNote cn = new CreditNote();
        cn.setId(UUID.randomUUID().toString());
        cn.setTenantId(tenantId);
        cn.setOrderId(orderId);
        cn.setAmount(amount);
        cn.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        cn.setType(result.tipo());
        cn.setCreatedByDni(createdByDni);
        cn.setCreatedByName(nameByDni(createdByDni));
        if (result.aprobado()) {
            cn.setCae(result.cae());
            cn.setCaeVencimiento(result.caeVencimiento());
            cn.setNumber(result.numero());
            cn.setPuntoVenta(result.puntoVenta());
            cn.setQrUrl(result.qrUrl());
        } else {
            cn.setError(result.error());
        }
        return repo.save(cn);
    }

    private String nameByDni(String dni) {
        if (dni == null || dni.isBlank()) return null;
        return adminUsers.findByDniForTenant(dni, TenantContext.getTenantId())
                .map(u -> u.getNombre() + " " + u.getApellido()).orElse(null);
    }
}
