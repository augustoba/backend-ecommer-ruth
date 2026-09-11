package com.estilospequenos.service;

import com.estilospequenos.model.PlatformMailSettings;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Wrapper fino para el mail de cupón de campaña. Arma el `JavaMailSender` al
 * momento de mandar cada mail, con las credenciales guardadas en
 * {@link PlatformMailSettings} (editables desde el panel por el superadmin, sin
 * redeploy). No atrapa excepciones: si el envío falla, la deja subir para que
 * {@link MarketingCampaignService} decida qué hacer (registrar el fallo y seguir
 * con el resto del lote).
 */
@Service
public class MarketingMailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;

    public MarketingMailService(PlatformMailSettingsService mailSettingsService,
                                 SiteSettingsService siteSettingsService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
    }

    public void sendCoupon(String toEmail, String couponCode, int discountPercent, LocalDate expiresAt) {
        PlatformMailSettings cfg = mailSettingsService.get();
        String storeName = siteSettingsService.get().getStoreName();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(cfg.getFromAddress());
        message.setTo(toEmail);
        message.setSubject("¡" + discountPercent + "% de descuento para vos en " + storeName + "!");
        message.setText(
                "¡Hola! 🧸\n\n"
                        + "Como agradecimiento por ser parte de " + storeName + ", te regalamos un cupón de "
                        + discountPercent + "% de descuento para tu próxima compra.\n\n"
                        + "Código: " + couponCode + "\n"
                        + "Válido hasta el " + expiresAt.format(DATE_FMT) + ".\n\n"
                        + "Usalo al finalizar tu pedido. ¡Te esperamos!\n\n"
                        + storeName);

        buildSender(cfg).send(message);
    }

    private JavaMailSenderImpl buildSender(PlatformMailSettings cfg) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getHost());
        sender.setPort(cfg.getPort());
        sender.setUsername(cfg.getUsername());
        sender.setPassword(cfg.getPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
