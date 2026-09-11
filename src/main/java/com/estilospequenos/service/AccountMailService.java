package com.estilospequenos.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/** Mails transaccionales de la cuenta del admin (hoy: contraseña nueva por "olvidé mi contraseña"). */
@Service
public class AccountMailService {

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;

    public AccountMailService(PlatformMailSettingsService mailSettingsService,
                               SiteSettingsService siteSettingsService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
    }

    public void sendTempPassword(String toEmail, String nombre, String tempPassword) {
        String storeName = siteSettingsService.get().getStoreName();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailSettingsService.get().getFromAddress());
        message.setTo(toEmail);
        message.setSubject("Tu contraseña nueva en " + storeName);
        message.setText(
                "Hola " + nombre + "!\n\n"
                        + "Te generamos una contraseña nueva para entrar al panel de " + storeName + ":\n\n"
                        + tempPassword + "\n\n"
                        + "Entrá con ella y cambiala por una que quieras desde \"Mi cuenta\".\n\n"
                        + "Si vos no pediste esto, avisale a quien administra el sitio.");

        mailSettingsService.buildSender().send(message);
    }
}
