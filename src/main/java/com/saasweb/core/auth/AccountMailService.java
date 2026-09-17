package com.saasweb.core.auth;

import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.platform.PlatformMailSettingsService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/** Mails transaccionales de la cuenta del admin (hoy: link de "olvidé mi contraseña"). */
@Service
public class AccountMailService {

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;

    public AccountMailService(PlatformMailSettingsService mailSettingsService,
                               SiteSettingsService siteSettingsService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
    }

    /** {@code link} incluye el token de un solo uso — vence a la hora, ver {@code AuthService}. */
    public void sendPasswordResetLink(String toEmail, String nombre, String link) {
        String storeName = siteSettingsService.get().getStoreName();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailSettingsService.get().getFromAddress());
        message.setTo(toEmail);
        message.setSubject("Recuperar tu contraseña en " + storeName);
        message.setText(
                "Hola " + nombre + "!\n\n"
                        + "Entrá a este link para elegir una contraseña nueva para el panel de " + storeName + ":\n\n"
                        + link + "\n\n"
                        + "Vence en 1 hora y sirve una sola vez.\n\n"
                        + "Si vos no pediste esto, ignorá este mail — tu contraseña actual sigue funcionando igual.");

        mailSettingsService.buildSender().send(message);
    }
}
