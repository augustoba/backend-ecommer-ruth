package com.saasweb.core.arca;

import com.saasweb.core.order.Order;
import com.saasweb.core.settings.SiteSettings;
import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.platform.PlatformMailSettings;
import com.saasweb.platform.PlatformMailSettingsService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Manda el comprobante (factura o ticket interno) en PDF al mail del
 * cliente — sólo si cargó uno en el checkout/POS. No bloquea la venta si
 * falla: {@code OrderService} llama a esto DESPUÉS de confirmar/facturar,
 * dentro de un try/catch que sólo loguea.
 */
@Service
public class InvoiceMailService {

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;
    private final InvoicePdfService pdfService;

    public InvoiceMailService(PlatformMailSettingsService mailSettingsService, SiteSettingsService siteSettingsService,
                              InvoicePdfService pdfService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
        this.pdfService = pdfService;
    }

    public void send(Order order) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) return;
        SiteSettings settings = siteSettingsService.get();
        byte[] pdf = pdfService.generate(order, settings);

        try {
            PlatformMailSettings mailCfg = mailSettingsService.get();
            JavaMailSenderImpl sender = mailSettingsService.buildSender();
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(mailCfg.getFromAddress());
            helper.setTo(order.getCustomerEmail());
            boolean esFactura = order.getInvoiceType() != null && order.getInvoiceType().startsWith("FACTURA_");
            helper.setSubject((esFactura ? "Tu factura de " : "Tu comprobante de ") + settings.getStoreName()
                    + " — " + order.getCode());
            helper.setText("¡Gracias por tu compra en " + settings.getStoreName() + "! Adjuntamos tu comprobante.", false);
            helper.addAttachment(order.getCode() + ".pdf", new ByteArrayResource(pdf));
            sender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo mandar el comprobante por mail: " + e.getMessage(), e);
        }
    }
}
