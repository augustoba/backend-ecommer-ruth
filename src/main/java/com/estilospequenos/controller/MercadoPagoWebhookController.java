package com.estilospequenos.controller;

import com.estilospequenos.model.SiteSettings;
import com.estilospequenos.service.MercadoPagoService;
import com.estilospequenos.service.OrderService;
import com.estilospequenos.service.SiteSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Notificaciones de pago de Mercado Pago — público, sin JWT (MP no manda
 * ninguna sesión nuestra).
 *
 * <p>Siempre responde 200 — Mercado Pago reintenta agresivo ante cualquier
 * respuesta que no sea 2xx, y reintentar no sirve de nada si el pedido no
 * se pudo confirmar (ej. se quedó sin stock entre que se creó el pedido y
 * se aprobó el pago) — esos casos quedan sólo logueados para revisar a
 * mano, no bloquean la respuesta a Mercado Pago.</p>
 */
@RestController
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final SiteSettingsService siteSettingsService;
    private final MercadoPagoService mercadoPagoService;
    private final OrderService orderService;

    public MercadoPagoWebhookController(SiteSettingsService siteSettingsService,
                                         MercadoPagoService mercadoPagoService,
                                         OrderService orderService) {
        this.siteSettingsService = siteSettingsService;
        this.mercadoPagoService = mercadoPagoService;
        this.orderService = orderService;
    }

    @PostMapping("/api/webhooks/mercadopago")
    public ResponseEntity<Void> receive(
            @RequestParam(name = "data.id", required = false) String dataIdParam,
            @RequestParam(name = "type", required = false) String typeParam,
            @RequestBody(required = false) Map<String, Object> body) {

        String type = typeParam != null ? typeParam : topicFromBody(body);
        if (!"payment".equals(type)) return ResponseEntity.ok().build();

        String paymentId = dataIdParam != null ? dataIdParam : paymentIdFromBody(body);
        if (paymentId == null || paymentId.isBlank()) return ResponseEntity.ok().build();

        SiteSettings settings = siteSettingsService.get();
        String accessToken = settings.getMpAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Webhook de Mercado Pago sin Access Token cargado — ignorado.");
            return ResponseEntity.ok().build();
        }

        MercadoPagoService.PaymentInfo payment = mercadoPagoService.getPayment(accessToken, paymentId);
        String orderId = payment.externalReference();
        if (orderId == null || orderId.isBlank()) return ResponseEntity.ok().build();

        try {
            switch (payment.status()) {
                case "approved" -> orderService.confirmFromPayment(orderId, payment.id());
                case "rejected", "cancelled" -> orderService.markPaymentRejected(orderId);
                default -> {
                    // "pending"/"in_process": todavía no hay nada que hacer, se espera la próxima notificación.
                }
            }
        } catch (RuntimeException e) {
            log.error("No se pudo aplicar el pago {} (status={}) al pedido {}: {}",
                    paymentId, payment.status(), orderId, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private static String topicFromBody(Map<String, Object> body) {
        if (body == null) return null;
        Object t = body.get("type");
        return t != null ? String.valueOf(t) : null;
    }

    private static String paymentIdFromBody(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> m && m.get("id") != null) return String.valueOf(m.get("id"));
        return null;
    }
}
