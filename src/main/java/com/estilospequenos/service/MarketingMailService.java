package com.estilospequenos.service;

import com.estilospequenos.model.MarketingConfig;
import com.estilospequenos.model.PlatformMailSettings;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Wrapper fino para el mail de cupón de campaña. Arma el `JavaMailSender` al
 * momento de mandar cada mail, con las credenciales guardadas en
 * {@link PlatformMailSettings} (editables desde el panel por el superadmin, sin
 * redeploy). El asunto, el texto y la imagen salen de {@link MarketingConfig}
 * (editables desde /admin/campanias) — si no se cargaron, usa un texto por
 * defecto. No atrapa excepciones: si el envío falla, la deja subir para que
 * {@link MarketingCampaignService} decida qué hacer (registrar el fallo y
 * seguir con el resto del lote).
 */
@Service
public class MarketingMailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DEFAULT_SUBJECT = "¡{porcentaje}% de descuento para vos en {tienda}!";
    private static final String DEFAULT_BODY =
            "¡Hola! 🧸\n\n"
                    + "Como agradecimiento por ser parte de {tienda}, te regalamos un cupón de "
                    + "{porcentaje}% de descuento para tu próxima compra.\n\n"
                    + "Código: {codigo}\n"
                    + "Válido hasta el {vencimiento}.\n\n"
                    + "Usalo al finalizar tu pedido. ¡Te esperamos!\n\n"
                    + "{tienda}";

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;

    public MarketingMailService(PlatformMailSettingsService mailSettingsService,
                                 SiteSettingsService siteSettingsService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
    }

    public void sendCoupon(String toEmail, MarketingConfig campaignCfg, String couponCode, LocalDate expiresAt) {
        PlatformMailSettings mailCfg = mailSettingsService.get();
        String storeName = siteSettingsService.get().getStoreName();

        String subjectTemplate = campaignCfg.getEmailSubject() != null && !campaignCfg.getEmailSubject().isBlank()
                ? campaignCfg.getEmailSubject() : DEFAULT_SUBJECT;
        String bodyTemplate = campaignCfg.getEmailBody() != null && !campaignCfg.getEmailBody().isBlank()
                ? campaignCfg.getEmailBody() : DEFAULT_BODY;

        String subject = render(subjectTemplate, storeName, couponCode, campaignCfg.getDiscountPercent(), expiresAt);
        String bodyText = render(bodyTemplate, storeName, couponCode, campaignCfg.getDiscountPercent(), expiresAt);
        String imageUrl = campaignCfg.getEmailImageUrl();

        try {
            JavaMailSenderImpl sender = mailSettingsService.buildSender();
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(mailCfg.getFromAddress());
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String html = "<div style=\"font-family:sans-serif;font-size:15px;color:#333;line-height:1.5;\">"
                    + (imageUrl != null && !imageUrl.isBlank()
                            ? "<img src=\"cid:banner\" alt=\"\" style=\"max-width:100%;margin-bottom:16px;\"/>" : "")
                    + bodyText.replace("\n", "<br/>")
                    + "</div>";
            helper.setText(bodyText, html);

            if (imageUrl != null && !imageUrl.isBlank()) {
                DataUriImage img = parseDataUri(imageUrl);
                helper.addInline("banner", new ByteArrayResource(img.bytes()), img.contentType());
            }

            sender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo armar/mandar el mail: " + e.getMessage(), e);
        }
    }

    private String render(String template, String storeName, String couponCode, int discountPercent, LocalDate expiresAt) {
        return template
                .replace("{tienda}", storeName)
                .replace("{codigo}", couponCode)
                .replace("{porcentaje}", String.valueOf(discountPercent))
                .replace("{vencimiento}", expiresAt.format(DATE_FMT));
    }

    private record DataUriImage(byte[] bytes, String contentType) {}

    /** Espera el formato "data:image/png;base64,AAAA...". */
    private DataUriImage parseDataUri(String dataUri) {
        int semicolon = dataUri.indexOf(';');
        int comma = dataUri.indexOf(',');
        String contentType = dataUri.substring("data:".length(), semicolon);
        byte[] bytes = Base64.getDecoder().decode(dataUri.substring(comma + 1));
        return new DataUriImage(bytes, contentType);
    }
}
