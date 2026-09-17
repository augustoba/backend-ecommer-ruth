package com.saasweb.core.arca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasweb.common.BadRequestException;
import com.saasweb.core.arca.ArcaWsaaClient.Ticket;
import com.saasweb.core.arca.ArcaWsfeClient.FacturaRequest;
import com.saasweb.core.arca.ArcaWsfeClient.FacturaResult;
import com.saasweb.core.plan.Modules;
import com.saasweb.core.plan.PlanService;
import com.saasweb.core.settings.SiteSettings;
import com.saasweb.core.settings.SiteSettingsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
 */
@Service
public class ArcaInvoiceService {

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

    public record InvoiceResult(
            boolean aprobado,
            /** "FACTURA_A" | "FACTURA_B" | "FACTURA_C" — el tipo que se INTENTÓ emitir, se haya aprobado o no. */
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
     * Pide el CAE de una factura por {@code importeTotal}. {@code docNroDni}
     * es opcional — null/0 = consumidor final sin identificar (válido hasta
     * el tope que fija ARCA para no identificar al comprador; si la venta
     * supera ese tope hay que cargar el DNI, no se valida acá todavía, ver
     * PLAN_SAAS.md Fase 14). {@code buyerCuit} también es opcional: sólo
     * tiene efecto si el tenant es Responsable Inscripto — en ese caso pasa
     * de emitir Factura B (consumidor final) a Factura A (a ese CUIT).
     */
    public InvoiceResult emitirFactura(String tenantId, BigDecimal importeTotal, Long docNroDni, String buyerCuit) {
        if (!isAvailable()) {
            throw new BadRequestException("Esta tienda todavía no configuró la facturación con ARCA.");
        }
        SiteSettings s = siteSettingsService.get();
        int cbteTipo = cbteTipoFor(s, buyerCuit);
        String tipoLabel = invoiceTypeLabel(cbteTipo);
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

            FacturaRequest req = new FacturaRequest(
                    s.getArcaPuntoVenta(), cbteTipo, next, fecha, importeTotal, docTipo, docNro);
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

    /** Monotributo/Exento → C (nunca discrimina IVA). Responsable Inscripto → A si hay CUIT del comprador, si no B. */
    private int cbteTipoFor(SiteSettings s, String buyerCuit) {
        if (!"RESPONSABLE_INSCRIPTO".equals(s.getArcaCondicionIva())) {
            return ArcaWsfeClient.CBTE_TIPO_FACTURA_C;
        }
        return notBlank(buyerCuit) ? ArcaWsfeClient.CBTE_TIPO_FACTURA_A : ArcaWsfeClient.CBTE_TIPO_FACTURA_B;
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
