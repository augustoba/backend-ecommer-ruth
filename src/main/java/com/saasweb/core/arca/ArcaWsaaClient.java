package com.saasweb.core.arca;

import com.saasweb.common.BadRequestException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.Store;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WSAA (Web Service de Autenticación y Autorización) de ARCA/AFIP — el paso
 * previo obligatorio antes de poder pedirle un CAE al WSFEv1 (ver
 * `ArcaWsfeClient`). No es un login con usuario/contraseña: hay que armar un
 * XML ("login ticket request"), firmarlo como CMS/PKCS#7 con el certificado
 * y la clave privada QUE EL TENANT ASOCIÓ a este servicio en su cuenta de
 * ARCA, y mandárselo a WSAA — a cambio da un Token + Sign válidos por ~12
 * horas que hay que adjuntar en cada llamada al WSFEv1. Documentación:
 * https://www.afip.gob.ar/ws/documentacion/wsaa.asp
 *
 * <p><b>Nada de esto se probó contra ARCA de verdad todavía</b> (ver
 * PLAN_SAAS.md Fase 14) — el usuario no tiene todavía un CUIT con un
 * certificado de homologación asociado al servicio `wsfe`. Está construido
 * siguiendo al pie de la letra el manual del desarrollador de WSAA, pero
 * hay un punto concreto marcado abajo que sólo se puede confirmar con una
 * prueba real: el algoritmo de firma del CMS.</p>
 */
@Component
class ArcaWsaaClient {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /** Token+Sign son válidos ~12hs; se cachean en memoria por tenant+servicio para no pedir uno nuevo en cada venta. */
    private final Map<String, CachedTicket> cache = new ConcurrentHashMap<>();

    record CachedTicket(String token, String sign, Instant expiresAt) {
        boolean isValid() {
            // Margen de 5 minutos para no arrancar una venta con un token que vence en el medio.
            return Instant.now().isBefore(expiresAt.minusSeconds(300));
        }
    }

    record Ticket(String token, String sign) {
    }

    /**
     * Devuelve un Token+Sign vigente para el servicio pedido (siempre
     * "wsfe" acá) — reusa el de la cache si todavía no venció, si no pide
     * uno nuevo a WSAA. {@code cacheKey} tiene que identificar al tenant
     * (más el modo prueba/producción, por si algún día conviven).
     */
    Ticket getTicket(String cacheKey, String certificadoPem, String clavePrivadaPem, boolean modoPrueba) {
        CachedTicket cached = cache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return new Ticket(cached.token(), cached.sign());
        }
        CachedTicket fresh = requestNewTicket(certificadoPem, clavePrivadaPem, modoPrueba);
        cache.put(cacheKey, fresh);
        return new Ticket(fresh.token(), fresh.sign());
    }

    private CachedTicket requestNewTicket(String certificadoPem, String clavePrivadaPem, boolean modoPrueba) {
        String traXml = buildLoginTicketRequest();
        String cms = signAsCms(traXml, certificadoPem, clavePrivadaPem);
        String soapResponse = callLoginCms(cms, modoPrueba);
        return parseLoginTicketResponse(soapResponse);
    }

    // --- 1) Armar el TRA (login ticket request) ---

    private String buildLoginTicketRequest() {
        Instant now = Instant.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        String generationTime = now.minusSeconds(60).atOffset(ZoneOffset.of("-03:00")).format(fmt);
        String expirationTime = now.plusSeconds(300).atOffset(ZoneOffset.of("-03:00")).format(fmt);
        long uniqueId = now.getEpochSecond() % 1_000_000_000L;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                + "<header>"
                + "<uniqueId>" + uniqueId + "</uniqueId>"
                + "<generationTime>" + generationTime + "</generationTime>"
                + "<expirationTime>" + expirationTime + "</expirationTime>"
                + "</header>"
                + "<service>wsfe</service>"
                + "</loginTicketRequest>";
    }

    // --- 2) Firmarlo como CMS/PKCS#7 (adjuntando el contenido) ---

    /**
     * ARCA exige firmar el TRA con el certificado/clave del tenant. El
     * manual histórico de WSAA pedía SHA-1; acá se usa SHA-256 (el estándar
     * moderno de facto en integraciones actuales) — <b>si el primer intento
     * real contra homologación devuelve un error de firma inválida, este es
     * el primer lugar a revisar</b> (cambiar "SHA256withRSA" por
     * "SHA1withRSA" más abajo).
     */
    private String signAsCms(String traXml, String certificadoPem, String clavePrivadaPem) {
        try {
            X509Certificate cert = parseCertificate(certificadoPem);
            PrivateKey key = parsePrivateKey(clavePrivadaPem);

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSimpleSignerInfoGeneratorBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build("SHA256withRSA", key, cert));
            List<X509Certificate> certList = new ArrayList<>();
            certList.add(cert);
            Store<?> certStore = new JcaCertStore(certList);
            generator.addCertificates(certStore);

            CMSTypedData content = new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8));
            CMSSignedData signedData = generator.generate(content, true); // true = adjunta el contenido original
            return Base64.getEncoder().encodeToString(signedData.getEncoded());
        } catch (Exception e) {
            throw new BadRequestException("No se pudo firmar el pedido a ARCA — revisá el certificado/clave cargados: " + e.getMessage());
        }
    }

    private X509Certificate parseCertificate(String pem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }

    /** Acepta tanto claves PKCS#1 ("RSA PRIVATE KEY", típico de `openssl genrsa`) como PKCS#8 ("PRIVATE KEY"). */
    private PrivateKey parsePrivateKey(String pem) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (obj instanceof PEMKeyPair pemKeyPair) {
                return converter.getKeyPair(pemKeyPair).getPrivate();
            }
            if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            }
            throw new IllegalArgumentException("Formato de clave privada no reconocido.");
        }
    }

    // --- 3) Mandárselo a WSAA por SOAP ---

    private final RestClient restClient = RestClient.create();

    private String callLoginCms(String cmsBase64, boolean modoPrueba) {
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:wsaa=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<wsaa:loginCms><wsaa:in0>" + cmsBase64 + "</wsaa:in0></wsaa:loginCms>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
        try {
            return restClient.post()
                    .uri(ArcaEndpoints.wsaaUrl(modoPrueba))
                    .header("SOAPAction", "")
                    .contentType(new MediaType("text", "xml", StandardCharsets.UTF_8))
                    .body(envelope)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new BadRequestException("ARCA (WSAA) rechazó el pedido de autenticación ("
                    + e.getStatusCode() + "): " + firstLine(e.getResponseBodyAsString()));
        }
    }

    // --- 4) Parsear la respuesta ---

    private CachedTicket parseLoginTicketResponse(String soapResponseXml) {
        try {
            Document soapDoc = parseXml(soapResponseXml);
            XPath xpath = XPathFactory.newInstance().newXPath();
            // El SOAP envuelve la respuesta real (otro XML) como texto escapado dentro de loginCmsReturn.
            String innerXml = (String) xpath.evaluate("//loginCmsReturn/text()", soapDoc, XPathConstants.STRING);
            if (innerXml == null || innerXml.isBlank()) {
                throw new BadRequestException("ARCA no devolvió un token válido: " + soapResponseXml);
            }
            Document ticketDoc = parseXml(innerXml);
            String token = (String) xpath.evaluate("//token/text()", ticketDoc, XPathConstants.STRING);
            String sign = (String) xpath.evaluate("//sign/text()", ticketDoc, XPathConstants.STRING);
            String expirationTime = (String) xpath.evaluate("//expirationTime/text()", ticketDoc, XPathConstants.STRING);
            if (token == null || token.isBlank() || sign == null || sign.isBlank()) {
                throw new BadRequestException("ARCA no devolvió token/sign: " + innerXml);
            }
            Instant expiresAt = java.time.OffsetDateTime.parse(expirationTime).toInstant();
            return new CachedTicket(token, sign, expiresAt);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("No se pudo interpretar la respuesta de ARCA (WSAA): " + e.getMessage());
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i > 0 ? s.substring(0, i) : s;
    }
}
