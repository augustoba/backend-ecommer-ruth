package com.estilospequenos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

/**
 * Borrado real de imágenes en Cloudinary (el upload preset del panel es
 * <b>unsigned</b>: sólo permite subir, nunca borrar). Hace falta una firma
 * SHA-1 con el API Secret de la cuenta — no hay SDK oficial de Java liviano
 * para esto, así que se arma la request firmada a mano contra el endpoint
 * REST de Cloudinary.
 * <p><b>Best-effort</b>: si falla (credenciales no cargadas, red caída,
 * imagen ya borrada de otro lado) nunca tira excepción — el borrado del
 * producto en la base sigue adelante igual. Ver {@link ProductService#permanentlyDelete}.</p>
 */
@Service
public class CloudinaryAdminService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryAdminService.class);

    private final SiteSettingsService settingsService;

    public CloudinaryAdminService(SiteSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** Borra estas imágenes de Cloudinary si hay credenciales cargadas. Nunca tira excepción. */
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        var settings = settingsService.get();
        String cloudName = settings.getCloudinaryCloudName();
        String apiKey = settings.getCloudinaryApiKey();
        String apiSecret = settings.getCloudinaryApiSecret();
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            log.info("Cloudinary API Key/Secret no configurados — no se borran las {} imagen(es) de la cuenta "
                    + "(sólo queda borrado el producto en la base).", imageUrls.size());
            return;
        }
        for (String url : imageUrls) {
            try {
                deleteOne(cloudName, apiKey, apiSecret, url);
            } catch (Exception e) {
                log.warn("No se pudo borrar de Cloudinary la imagen {}: {}", url, e.getMessage());
            }
        }
    }

    private void deleteOne(String cloudName, String apiKey, String apiSecret, String url) throws NoSuchAlgorithmException {
        String publicId = extractPublicId(url);
        if (publicId == null) return;

        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1Hex("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("public_id", publicId);
        form.add("api_key", apiKey);
        form.add("timestamp", String.valueOf(timestamp));
        form.add("signature", signature);

        RestClient.create()
                .post()
                .uri("https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * De una URL como {@code https://res.cloudinary.com/<cloud>/image/upload/v169.../carpeta/nombre.jpg}
     * saca el public_id ({@code carpeta/nombre}, sin versión ni extensión).
     * {@code null} si la URL no es de Cloudinary (foto pegada a mano, data URI, etc).
     */
    static String extractPublicId(String url) {
        if (url == null) return null;
        String marker = "/image/upload/";
        int at = url.indexOf(marker);
        if (at == -1) return null;
        String after = url.substring(at + marker.length());
        int slash = after.indexOf('/');
        String rest = (slash != -1 && after.substring(0, slash).matches("v\\d+"))
                ? after.substring(slash + 1)
                : after;
        int dot = rest.lastIndexOf('.');
        return dot > 0 ? rest.substring(0, dot) : rest;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String sha1Hex(String input) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
