package com.saasweb.core.arca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasweb.common.BadRequestException;
import com.saasweb.core.arca.ArcaWsaaClient.Ticket;
import com.saasweb.core.arca.ArcaWsfeClient.CbteAsociado;
import com.saasweb.core.arca.ArcaWsfeClient.FacturaRequest;
import com.saasweb.core.arca.ArcaWsfeClient.FacturaResult;
import com.saasweb.core.arca.ArcaWsfeClient.IvaGroup;
import com.saasweb.core.plan.Modules;
import com.saasweb.core.plan.PlanService;
import com.saasweb.core.settings.SiteSettings;
import com.saasweb.core.settings.SiteSettingsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Punto de entrada para el resto del backend (ver `OrderService`) — arma el
 * flujo completo (WSAA → último número → CAE) y devuelve un resultado que
 * SIEMPRE hay que persistir en el pedido, apruebe o no: si ARCA rechaza el
 * comprobante o falla la conexión, la venta ya pasó (el cliente pagó) y no
 * tiene sentido bloquearla — queda marcada para reintentar a mano (ver
 * `Order.invoiceError`, `OrderService.retryInvoicing`).
 *
 * <p>Elige el tipo de comprobante según la condición frente al IVA que el
 * propio tenant cargó en Configuración: Monotributo/Exento → Factura C (no
 * discrimina IVA). Responsable Inscripto → Factura B a consumidor final, o
 * Factura A si se cargó el CUIT del comprador al cobrar.</p>
 *
 * <p><b>Fase 15:</b> el IVA discriminado (A/B) ahora se arma por alícuota
 * REAL de cada línea del pedido (antes era siempre 21% fijo) — ver
 * {@link #emitirFactura(String, java.util.List, Long, String)}. También suma
 * Notas de Crédito ({@link #emitirNotaCredito}).</p>
 */
@Service
public class ArcaInvoiceService {

    /** IDs de alícuota que usa WSFEv1 (tabla oficial de AFIP). */
    private static final Map<String, String> ALICUOTA_IDS = Map.of(
            "21.0", "5", "21", "5",
            "10.5", "4",
            "5.0", "8", "5", "8",
            "2.5", "9",
            "0.0", "3", "0", "3");
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("21");

    private final ArcaWsaaClient wsaaClient;
    private final ArcaWsfeClient wsfeClient;
    private final SiteSettingsService siteSettingsService;
    private final PlanService planService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArcaInvoiceService(ArcaWsaaClient wsaaClient, ArcaWsfeClient wsfeClient,
                               SiteSettingsService siteSettingsService, PlanService planService) {
        this.wsaaClient = wsaaClient;
        this.wsfeClient = wsfeClient;
        this.siteSettingsService = siteSettingsService;
        this.planService = planService;
    }

    /** Una línea del pedido tal como la ve ARCA: importe + alícuota de IVA de ese producto. */
    public record InvoiceLine(BigDecimal amount, BigDecimal ivaRatePercent) {}

    public record InvoiceResult(
            boolean aprobado,
            /** "FACTURA_A" | "FACTURA_B" | "FACTURA_C" | "NC_A" | "NC_B" | "NC_C" — lo que se INTENTÓ emitir. */
            String tipo,
            String cae,
            String caeVencimiento,
            Long numero,
            Integer puntoVenta,
            String qrUrl,
            /** Motivo del rechazo/falla — null si aprobado. */
            String error
    ) {
    }

    /** true si el módulo está habilitado en el plan Y el tenant cargó CUIT + certificado + punto de venta. */
    public boolean isAvailable() {
        var plan = planService.getCurrent();
        if (plan == null || !plan.hasModule(Modules.ARCA_INVOICING)) return false;
        SiteSettings s = siteSettingsService.get();
        return s.isArcaEnabled()
                && notBlank(s.getArcaCuit())
                && notBlank(s.getArcaCertificadoPem())
                && notBlank(s.getArcaClavePrivadaPem())
                && s.getArcaPuntoVenta() != null;
    }

    /**
     * Pide el CAE de una factura por las líneas del pedido (cada una con su
     * importe y alícuota de IVA real — ver {@link InvoiceLine}). {@code
     * docNroDni} es opcional — null/0 = consumidor final sin identificar.
     * {@code buyerCuit} también es opcional: sólo tiene efecto si el tenant
     * es Responsable Inscripto — en ese caso pasa de emitir Factura B
     * (consumidor final) a Factura A (a ese CUIT).
     */
    public InvoiceResult emitirFactura(String tenantId, java.util.List<InvoiceLine> lines, Long docNroDni, String buyerCuit) {
        if (!isAvailable()) {
            throw new BadRequestException("Esta tienda todavía no configuró la facturación con ARCA.");
        }
        SiteSettings s = siteSettingsService.get();
        int cbteTipo = cbteTipoFor(s, buyerCuit);
        String tipoLabel = invoiceTypeLabel(cbteTipo);
        BigDecimal importeTotal = lines.stream().map(InvoiceLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        try {
            Ticket ticket = wsaaClient.getTicket(
                    tenantId + (s.isArcaModoPrueba() ? ":homo" : ":prod"),
                    s.getArcaCertificadoPem(), s.getArcaClavePrivadaPem(), s.isArcaModoPrueba());

            long last = wsfeClient.getLastAuthorized(ticket, s.getArcaCuit(), s.getArcaPuntoVenta(),
                    cbteTipo, s.isArcaModoPrueba());
            long next = last + 1;

            int docTipo;
            long docNro;
            if (cbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_A) {
                docTipo = ArcaWsfeClient.DOC_TIPO_CUIT;
                docNro = Long.parseLong(buyerCuit.replaceAll("\\D", ""));
            } else {
                docTipo = docNroDni != null && docNroDni > 0
                        ? ArcaWsfeClient.DOC_TIPO_DNI : ArcaWsfeClient.DOC_TIPO_CONSUMIDOR_FINAL;
                docNro = docNroDni != null ? docNroDni : 0;
            }
            String fecha = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());

            boolean discrimina = cbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_A
                    || cbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_B;
            var ivaGroups = discrimina ? ivaGroupsFor(lines) : java.util.List.<IvaGroup>of();

            FacturaRequest req = new FacturaRequest(
                    s.getArcaPuntoVenta(), cbteTipo, next, fecha, importeTotal, docTipo, docNro, ivaGroups, null);
            FacturaResult result = wsfeClient.solicitarCae(ticket, s.getArcaCuit(), req, s.isArcaModoPrueba());

            if (!result.aprobado()) {
                String obs = String.join("; ", result.observaciones());
                return new InvoiceResult(false, tipoLabel, null, null, next, s.getArcaPuntoVenta(), null,
                        obs.isBlank() ? "ARCA rechazó el comprobante." : obs);
            }
            String qrUrl = buildQrUrl(s.getArcaCuit(), s.getArcaPuntoVenta(), next, importeTotal,
                    cbteTipo, docTipo, docNro, result.cae());
            return new InvoiceResult(true, tipoLabel, result.cae(), result.caeVencimiento(), next,
                    s.getArcaPuntoVenta(), qrUrl, null);
        } catch (BadRequestException e) {
            // No relanzar: quien llama (OrderService) tiene que poder guardar la venta igual, con el error anotado.
            return new InvoiceResult(false, tipoLabel, null, null, null, s.getArcaPuntoVenta(), null, e.getMessage());
        }
    }

    /**
     * Nota de crédito contra una Factura ARCA ya aprobada. Simplificación
     * deliberada (igual que la factura original no tiene ítems detallados):
     * el monto es un importe único, sin desglose por producto — si la
     * factura original discriminaba IVA (A/B), la NC discrimina también,
     * a una única alícuota general del 21% (no se guarda qué alícuotas
     * tenía cada línea de la venta original).
     */
    public InvoiceResult emitirNotaCredito(String tenantId, String originalInvoiceType, int originalPuntoVenta,
                                           long originalNumero, String originalBuyerCuit, BigDecimal amount) {
        if (!isAvailable()) {
            throw new BadRequestException("Esta tienda todavía no configuró la facturación con ARCA.");
        }
        int origCbteTipo = cbteTipoForInvoiceType(originalInvoiceType);
        int ncCbteTipo = ncTipoFor(origCbteTipo);
        String tipoLabel = "NC_" + originalInvoiceType.substring(originalInvoiceType.length() - 1);
        SiteSettings s = siteSettingsService.get();
        try {
            Ticket ticket = wsaaClient.getTicket(
                    tenantId + (s.isArcaModoPrueba() ? ":homo" : ":prod"),
                    s.getArcaCertificadoPem(), s.getArcaClavePrivadaPem(), s.isArcaModoPrueba());

            long last = wsfeClient.getLastAuthorized(ticket, s.getArcaCuit(), s.getArcaPuntoVenta(),
                    ncCbteTipo, s.isArcaModoPrueba());
            long next = last + 1;

            int docTipo;
            long docNro;
            if (origCbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_A) {
                docTipo = ArcaWsfeClient.DOC_TIPO_CUIT;
                docNro = originalBuyerCuit != null ? Long.parseLong(originalBuyerCuit.replaceAll("\\D", "")) : 0;
            } else {
                docTipo = ArcaWsfeClient.DOC_TIPO_CONSUMIDOR_FINAL;
                docNro = 0;
            }
            String fecha = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());

            boolean discrimina = origCbteTipo != ArcaWsfeClient.CBTE_TIPO_FACTURA_C;
            var ivaGroups = discrimina
                    ? java.util.List.of(ivaGroupFor(amount, DEFAULT_RATE))
                    : java.util.List.<IvaGroup>of();

            FacturaRequest req = new FacturaRequest(s.getArcaPuntoVenta(), ncCbteTipo, next, fecha, amount,
                    docTipo, docNro, ivaGroups,
                    new CbteAsociado(origCbteTipo, originalPuntoVenta, originalNumero));
            FacturaResult result = wsfeClient.solicitarCae(ticket, s.getArcaCuit(), req, s.isArcaModoPrueba());

            if (!result.aprobado()) {
                String obs = String.join("; ", result.observaciones());
                return new InvoiceResult(false, tipoLabel, null, null, next, s.getArcaPuntoVenta(), null,
                        obs.isBlank() ? "ARCA rechazó la nota de crédito." : obs);
            }
            String qrUrl = buildQrUrl(s.getArcaCuit(), s.getArcaPuntoVenta(), next, amount,
                    ncCbteTipo, docTipo, docNro, result.cae());
            return new InvoiceResult(true, tipoLabel, result.cae(), result.caeVencimiento(), next,
                    s.getArcaPuntoVenta(), qrUrl, null);
        } catch (BadRequestException e) {
            return new InvoiceResult(false, tipoLabel, null, null, null, s.getArcaPuntoVenta(), null, e.getMessage());
        }
    }

    /** Agrupa las líneas por alícuota real y calcula neto/IVA de cada grupo (Neto = Importe / (1 + tasa/100)). */
    private java.util.List<IvaGroup> ivaGroupsFor(java.util.List<InvoiceLine> lines) {
        Map<BigDecimal, BigDecimal> byRate = new LinkedHashMap<>();
        for (InvoiceLine l : lines) {
            BigDecimal rate = l.ivaRatePercent() != null ? l.ivaRatePercent() : DEFAULT_RATE;
            byRate.merge(rate, l.amount(), BigDecimal::add);
        }
        java.util.List<IvaGroup> groups = new java.util.ArrayList<>();
        for (var e : byRate.entrySet()) {
            groups.add(ivaGroupFor(e.getValue(), e.getKey()));
        }
        return groups;
    }

    private IvaGroup ivaGroupFor(BigDecimal amount, BigDecimal ratePercent) {
        BigDecimal factor = BigDecimal.ONE.add(ratePercent.divide(new BigDecimal("100")));
        BigDecimal neto = factor.signum() == 0 ? amount : amount.divide(factor, 2, RoundingMode.HALF_UP);
        BigDecimal iva = amount.subtract(neto);
        String alicId = ALICUOTA_IDS.getOrDefault(ratePercent.stripTrailingZeros().toPlainString(), "5");
        return new IvaGroup(alicId, neto, iva);
    }

    /** Monotributo/Exento → C (nunca discrimina IVA). Responsable Inscripto → A si hay CUIT del comprador, si no B. */
    private int cbteTipoFor(SiteSettings s, String buyerCuit) {
        if (!"RESPONSABLE_INSCRIPTO".equals(s.getArcaCondicionIva())) {
            return ArcaWsfeClient.CBTE_TIPO_FACTURA_C;
        }
        return notBlank(buyerCuit) ? ArcaWsfeClient.CBTE_TIPO_FACTURA_A : ArcaWsfeClient.CBTE_TIPO_FACTURA_B;
    }

    private int cbteTipoForInvoiceType(String invoiceType) {
        return switch (invoiceType) {
            case "FACTURA_A" -> ArcaWsfeClient.CBTE_TIPO_FACTURA_A;
            case "FACTURA_B" -> ArcaWsfeClient.CBTE_TIPO_FACTURA_B;
            case "FACTURA_C" -> ArcaWsfeClient.CBTE_TIPO_FACTURA_C;
            default -> throw new BadRequestException("Este pedido no tiene una Factura ARCA aprobada.");
        };
    }

    private int ncTipoFor(int origCbteTipo) {
        if (origCbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_A) return ArcaWsfeClient.CBTE_TIPO_NC_A;
        if (origCbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_B) return ArcaWsfeClient.CBTE_TIPO_NC_B;
        return ArcaWsfeClient.CBTE_TIPO_NC_C;
    }

    private String invoiceTypeLabel(int cbteTipo) {
        if (cbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_A) return "FACTURA_A";
        if (cbteTipo == ArcaWsfeClient.CBTE_TIPO_FACTURA_B) return "FACTURA_B";
        return "FACTURA_C";
    }

    /**
     * QR obligatorio en todo comprobante (RG 4892) — un link a
     * afip.gob.ar/fe/qr con los datos del comprobante codificados en la URL
     * (base64 de un JSON), para que cualquiera lo pueda validar escaneándolo.
     */
    private String buildQrUrl(String cuit, int puntoVenta, long numero, BigDecimal importe,
                               int cbteTipo, int docTipo, long docNro, String cae) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ver", 1);
            data.put("fecha", LocalDate.now().toString());
            data.put("cuit", Long.parseLong(cuit));
            data.put("ptoVta", puntoVenta);
            data.put("tipoCmp", cbteTipo);
            data.put("nroCmp", numero);
            data.put("importe", importe);
            data.put("moneda", "PES");
            data.put("ctz", 1);
            data.put("tipoDocRec", docTipo);
            data.put("nroDocRec", docNro);
            data.put("tipoCodAut", "E");
            data.put("codAut", Long.parseLong(cae));
            String json = objectMapper.writeValueAsString(data);
            String base64 = Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "https://www.afip.gob.ar/fe/qr/?p=" + base64;
        } catch (Exception e) {
            return null; // el QR es un plus para el ticket impreso, no bloquea nada si falla armarlo
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
