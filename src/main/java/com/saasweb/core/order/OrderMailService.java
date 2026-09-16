package com.saasweb.core.order;

import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.platform.PlatformMailSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Mail de confirmación de un pedido pagado online (Fase 13, Mercado Pago) —
 * a diferencia del resto de los medios de pago (coordinados por WhatsApp,
 * que ya le queda al cliente como "recibo" en su propio chat), un pedido
 * pagado por Mercado Pago no deja ningún registro fuera del sitio, así que
 * hace falta mandarle uno por mail para que tenga con qué hacer un reclamo
 * si algo sale mal. Mismo patrón que {@code AccountMailService}: usa el
 * relay SMTP de PLATAFORMA (`PlatformMailSettingsService`), no el SMTP por
 * tenant (ese es sólo para recuperar contraseña de admins).
 */
@Service
public class OrderMailService {

    private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;

    public OrderMailService(PlatformMailSettingsService mailSettingsService, SiteSettingsService siteSettingsService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
    }

    /**
     * No hace nada si el pedido no tiene mail cargado — no debería pasar
     * para `MERCADOPAGO` (el checkout lo exige), pero por las dudas no
     * revienta la confirmación del pago por esto.
     */
    public void sendOrderConfirmation(Order order) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) return;

        String storeName = siteSettingsService.get().getStoreName();
        StringBuilder body = new StringBuilder();
        body.append("¡Hola ").append(order.getCustomerName()).append("!\n\n");
        body.append("Tu pago se acreditó y tu pedido en ").append(storeName).append(" quedó confirmado.\n\n");
        body.append("Código de pedido: ").append(order.getCode()).append("\n\n");
        for (OrderLine l : order.getLines()) {
            body.append("- ").append(l.getProductName()).append(" — talle ").append(l.getSize())
                    .append(" x").append(l.getQuantity()).append('\n');
        }
        body.append("\nTotal: $").append(order.getTotal()).append("\n\n");
        body.append("Guardá este mail como comprobante — cualquier consulta o reclamo sobre este pedido, ")
                .append("respondé acá o escribinos por WhatsApp mencionando el código.");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailSettingsService.get().getFromAddress());
            message.setTo(order.getCustomerEmail());
            message.setSubject("Confirmación de tu pedido " + order.getCode() + " — " + storeName);
            message.setText(body.toString());
            mailSettingsService.buildSender().send(message);
        } catch (RuntimeException e) {
            // Un mail que no sale no debe tirar abajo la confirmación del pago en sí.
            log.error("No se pudo mandar el mail de confirmación del pedido {}: {}", order.getCode(), e.getMessage());
        }
    }
}
