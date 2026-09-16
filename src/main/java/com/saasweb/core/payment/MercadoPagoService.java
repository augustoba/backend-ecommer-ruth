package com.saasweb.core.payment;

import com.saasweb.common.BadRequestException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API de Mercado Pago (Checkout Pro — ver PLAN_SAAS.md Fase
 * 13). Cada llamada recibe el Access Token del TENANT como parámetro (no
 * hay una cuenta de plataforma acá, a diferencia de Cloudinary) — lo
 * resuelve el caller desde `SiteSettings.mpAccessToken`.
 */
@Service
public class MercadoPagoService {

    private static final String API_BASE = "https://api.mercadopago.com";

    private final RestClient restClient = RestClient.create(API_BASE);

    public record PreferenceItem(String title, int quantity, BigDecimal unitPrice) {}

    public record PreferenceResult(String preferenceId, String initPoint) {}

    /**
     * Crea una preferencia de Checkout Pro para un pedido — el cliente se
     * redirige a `initPoint` para pagar en la página de Mercado Pago.
     * `externalReference` (el id del pedido) es cómo se lo resuelve de
     * vuelta cuando llega el pago (ver `MercadoPagoWebhookController`).
     */
    public PreferenceResult createPreference(
            String accessToken, String externalReference, List<PreferenceItem> items,
            String notificationUrl, String successUrl, String pendingUrl, String failureUrl) {

        List<Map<String, Object>> mpItems = items.stream()
                .map(i -> Map.<String, Object>of(
                        "title", i.title(),
                        "quantity", i.quantity(),
                        "unit_price", i.unitPrice(),
                        "currency_id", "ARS"))
                .toList();

        Map<String, Object> body = Map.of(
                "items", mpItems,
                "external_reference", externalReference,
                "notification_url", notificationUrl,
                "back_urls", Map.of(
                        "success", successUrl,
                        "pending", pendingUrl,
                        "failure", failureUrl),
                "auto_return", "approved");

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            String id = String.valueOf(response.get("id"));
            String initPoint = String.valueOf(response.get("init_point"));
            return new PreferenceResult(id, initPoint);
        } catch (RestClientResponseException e) {
            throw new BadRequestException(
                    "No se pudo iniciar el pago con Mercado Pago (" + e.getStatusCode()
                            + "). Revisá el Access Token cargado en \"Medios de pago\".");
        }
    }

    public record PaymentInfo(String id, String status, String externalReference) {}

    /**
     * Trae el pago DIRECTO de la API de Mercado Pago, autenticado con el
     * Access Token del tenant — nunca hay que confiar en el cuerpo de la
     * notificación del webhook (sólo trae el id), siempre hay que volver a
     * pedir el dato real acá para no poder ser falseado.
     */
    public PaymentInfo getPayment(String accessToken, String paymentId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            String status = String.valueOf(response.get("status"));
            Object ref = response.get("external_reference");
            return new PaymentInfo(paymentId, status, ref != null ? String.valueOf(ref) : null);
        } catch (RestClientResponseException e) {
            throw new BadRequestException("No se pudo consultar el pago en Mercado Pago (" + e.getStatusCode() + ").");
        }
    }
}
