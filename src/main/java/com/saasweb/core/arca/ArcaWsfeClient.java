package com.saasweb.core.arca;

import com.saasweb.common.BadRequestException;
import com.saasweb.core.arca.ArcaWsaaClient.Ticket;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * WSFEv1 (Web Service de Factura Electrónica) de ARCA/AFIP — el servicio de
 * negocio en sí: pide el último número autorizado y solicita el CAE de un
 * comprobante nuevo. Requiere el Token/Sign que da {@link ArcaWsaaClient}.
 * Documentación: https://www.afip.gob.ar/ws/documentacion/ws-factura-electronica.asp
 *
 * <p>Alcance (ver PLAN_SAAS.md Fase 14): Factura C (tipo 11, sin discriminar
 * IVA — Monotributista/Exento), Factura B (tipo 6, a consumidor final/DNI,
 * con IVA discriminado al 21% general) y Factura A (tipo 1, con CUIT del
 * comprador, misma discriminación de IVA) para Responsable Inscripto. Sin
 * ítems detallados en ningún caso (un solo importe total, no hay
 * `FeDetReq`/líneas) — {@link ArcaInvoiceService} decide el tipo según la
 * condición frente al IVA del tenant y si se cargó CUIT del comprador.</p>
 */
@Component
class ArcaWsfeClient {

    static final int CBTE_TIPO_FACTURA_A = 1;
    static final int CBTE_TIPO_FACTURA_B = 6;
    static final int CBTE_TIPO_FACTURA_C = 11;

    /** Consumidor final sin identificar (no hace falta CUIT/DNI del comprador). */
    static final int DOC_TIPO_CONSUMIDOR_FINAL = 99;
    static final int DOC_TIPO_DNI = 96;
    static final int DOC_TIPO_CUIT = 80;

    /** Único código de alícuota que usa esta integración: 21% (IVA general) — el que corresponde a la inmensa mayoría de la mercadería. */
    private static final String ALIC_IVA_21_ID = "5";
    private static final BigDecimal ALICUOTA_21 = new BigDecimal("1.21");

    private final RestClient restClient = RestClient.create();

    record FacturaRequest(
            int puntoVenta,
            int cbteTipo,
            long numero,
            /** yyyyMMdd */
            String fecha,
            BigDecimal importeTotal,
            int docTipo,
            long docNro
    ) {
        /** true si este tipo de comprobante discrimina IVA (A y B) — C nunca. */
        boolean discriminaIva() {
            return cbteTipo == CBTE_TIPO_FACTURA_A || cbteTipo == CBTE_TIPO_FACTURA_B;
        }

        /** Neto = total / 1.21 (IVA general, único que soporta esta integración), redondeado a 2 decimales. */
        BigDecimal impNeto() {
            return discriminaIva()
                    ? importeTotal.divide(ALICUOTA_21, 2, RoundingMode.HALF_UP)
                    : importeTotal;
        }

        BigDecimal impIva() {
            return discriminaIva() ? importeTotal.subtract(impNeto()) : BigDecimal.ZERO;
        }
    }

    record FacturaResult(
            boolean aprobado,
            String cae,
            /** yyyyMMdd */
            String caeVencimiento,
            List<String> observaciones
    ) {
    }

    /** Último número de comprobante autorizado para ese punto de venta/tipo — el próximo a pedir es éste + 1. */
    long getLastAuthorized(Ticket ticket, String cuit, int puntoVenta, int cbteTipo, boolean modoPrueba) {
        String body = soapEnvelope("""
                <ar:FECompUltimoAutorizado>
                  <ar:Auth>%s</ar:Auth>
                  <ar:PtoVta>%d</ar:PtoVta>
                  <ar:CbteTipo>%d</ar:CbteTipo>
                </ar:FECompUltimoAutorizado>
                """.formatted(authXml(ticket, cuit), puntoVenta, cbteTipo));
        Document response = call(body, "FECompUltimoAutorizado", modoPrueba);
        checkForErrors(response);
        String cbteNro = xpath(response, "//FECompUltimoAutorizadoResult/CbteNro/text()");
        return cbteNro == null || cbteNro.isBlank() ? 0 : Long.parseLong(cbteNro);
    }

    /** Pide el CAE del comprobante — {@code req.numero()} tiene que ser {@link #getLastAuthorized} + 1. */
    FacturaResult solicitarCae(Ticket ticket, String cuit, FacturaRequest req, boolean modoPrueba) {
        String importeTotal = req.importeTotal().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String importeNeto = req.impNeto().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String importeIva = req.impIva().setScale(2, RoundingMode.HALF_UP).toPlainString();
        // Factura A/B: WSFEv1 exige el array <Iva> con la alícuota discriminada. Factura C no discrimina IVA, no lo lleva.
        String ivaBlock = req.discriminaIva() ? """
                <ar:Iva>
                  <ar:AlicIva>
                    <ar:Id>%s</ar:Id>
                    <ar:BaseImp>%s</ar:BaseImp>
                    <ar:Importe>%s</ar:Importe>
                  </ar:AlicIva>
                </ar:Iva>
                """.formatted(ALIC_IVA_21_ID, importeNeto, importeIva) : "";
        String body = soapEnvelope("""
                <ar:FECAESolicitar>
                  <ar:Auth>%s</ar:Auth>
                  <ar:FeCAEReq>
                    <ar:FeCabReq>
                      <ar:CantReg>1</ar:CantReg>
                      <ar:PtoVta>%d</ar:PtoVta>
                      <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FeCabReq>
                    <ar:FeDetReq>
                      <ar:FECAEDetRequest>
                        <ar:Concepto>1</ar:Concepto>
                        <ar:DocTipo>%d</ar:DocTipo>
                        <ar:DocNro>%d</ar:DocNro>
                        <ar:CbteDesde>%d</ar:CbteDesde>
                        <ar:CbteHasta>%d</ar:CbteHasta>
                        <ar:CbteFch>%s</ar:CbteFch>
                        <ar:ImpTotal>%s</ar:ImpTotal>
                        <ar:ImpTotConc>0</ar:ImpTotConc>
                        <ar:ImpNeto>%s</ar:ImpNeto>
                        <ar:ImpOpEx>0</ar:ImpOpEx>
                        <ar:ImpIVA>%s</ar:ImpIVA>
                        <ar:ImpTrib>0</ar:ImpTrib>
                        <ar:MonId>PES</ar:MonId>
                        <ar:MonCotiz>1</ar:MonCotiz>
                        %s
                      </ar:FECAEDetRequest>
                    </ar:FeDetReq>
                  </ar:FeCAEReq>
                </ar:FECAESolicitar>
                """.formatted(authXml(ticket, cuit), req.puntoVenta(), req.cbteTipo(),
                req.docTipo(), req.docNro(), req.numero(), req.numero(), req.fecha(),
                importeTotal, importeNeto, importeIva, ivaBlock));
        Document response = call(body, "FECAESolicitar", modoPrueba);
        checkForErrors(response);

        String resultado = xpath(response, "//FECAESolicitarResult/FeDetResp/FECAEDetResponse/Resultado/text()");
        String cae = xpath(response, "//FECAESolicitarResult/FeDetResp/FECAEDetResponse/CAE/text()");
        String vencimiento = xpath(response, "//FECAESolicitarResult/FeDetResp/FECAEDetResponse/CAEFchVto/text()");
        List<String> observaciones = xpathList(response,
                "//FECAESolicitarResult/FeDetResp/FECAEDetResponse/Observaciones/Obs/Msg/text()");
        return new FacturaResult("A".equals(resultado), cae, vencimiento, observaciones);
    }

    private String authXml(Ticket ticket, String cuit) {
        return "<ar:Token>" + esc(ticket.token()) + "</ar:Token>"
                + "<ar:Sign>" + esc(ticket.sign()) + "</ar:Sign>"
                + "<ar:Cuit>" + cuit + "</ar:Cuit>";
    }

    private String soapEnvelope(String bodyXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ar=\"http://ar.gov.afip.dif.facturaelectronica/\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>" + bodyXml + "</soapenv:Body>"
                + "</soapenv:Envelope>";
    }

    private Document call(String soapBody, String operation, boolean modoPrueba) {
        try {
            String responseXml = restClient.post()
                    .uri(ArcaEndpoints.wsfeUrl(modoPrueba))
                    .header("SOAPAction", "http://ar.gov.afip.dif.facturaelectronica/" + operation)
                    .contentType(new MediaType("text", "xml", StandardCharsets.UTF_8))
                    .body(soapBody)
                    .retrieve()
                    .body(String.class);
            return parseXml(responseXml);
        } catch (RestClientResponseException e) {
            throw new BadRequestException("ARCA (WSFE) rechazó el pedido (" + e.getStatusCode() + "): "
                    + firstLine(e.getResponseBodyAsString()));
        } catch (Exception e) {
            throw new BadRequestException("No se pudo interpretar la respuesta de ARCA (WSFE): " + e.getMessage());
        }
    }

    /** Errores a nivel de la request entera (auth vencida, CUIT sin autorizar, formato inválido...), no del comprobante puntual. */
    private void checkForErrors(Document response) {
        NodeList errors = (NodeList) evaluate(response, "//Errors/Err", XPathConstants.NODESET);
        if (errors == null || errors.getLength() == 0) return;
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < errors.getLength(); i++) {
            Node err = errors.item(i);
            String code = textOf(err, "Code");
            String msg = textOf(err, "Msg");
            messages.add((code != null ? code + ": " : "") + msg);
        }
        throw new BadRequestException("ARCA devolvió un error: " + String.join("; ", messages));
    }

    private String textOf(Node parent, String childTag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (childTag.equals(child.getNodeName())) return child.getTextContent();
        }
        return null;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String xpath(Document doc, String expr) {
        try {
            return (String) evaluate(doc, expr, XPathConstants.STRING);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> xpathList(Document doc, String expr) {
        List<String> out = new ArrayList<>();
        try {
            NodeList nodes = (NodeList) evaluate(doc, expr, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) out.add(nodes.item(i).getTextContent());
        } catch (Exception ignored) {
            // sin observaciones, no es un error
        }
        return out;
    }

    private Object evaluate(Document doc, String expr, javax.xml.namespace.QName type) {
        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            return xp.evaluate(expr, doc, type);
        } catch (Exception e) {
            throw new BadRequestException("No se pudo leer la respuesta de ARCA: " + e.getMessage());
        }
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i > 0 ? s.substring(0, i) : s;
    }
}
