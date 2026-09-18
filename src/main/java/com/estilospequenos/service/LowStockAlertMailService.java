package com.estilospequenos.service;

import com.estilospequenos.dto.DashboardDtos.LowStockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

/** Mail diario de "talles por reponer" — ver {@link LowStockAlertScheduler}. */
@Service
public class LowStockAlertMailService {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertMailService.class);

    private final PlatformMailSettingsService mailSettingsService;
    private final SiteSettingsService siteSettingsService;

    public LowStockAlertMailService(PlatformMailSettingsService mailSettingsService,
                                    SiteSettingsService siteSettingsService) {
        this.mailSettingsService = mailSettingsService;
        this.siteSettingsService = siteSettingsService;
    }

    public void send(String toEmail, List<LowStockItem> items) {
        String storeName = siteSettingsService.get().getStoreName();

        StringBuilder rows = new StringBuilder();
        for (LowStockItem it : items) {
            rows.append("<tr><td style=\"padding:4px 8px;\">").append(it.productName())
                    .append("</td><td style=\"padding:4px 8px;\">").append(it.size())
                    .append("</td><td style=\"padding:4px 8px;text-align:center;\">").append(it.stock())
                    .append("</td></tr>");
        }
        String html = "<div style=\"font-family:sans-serif;font-size:14px;color:#333;\">"
                + "<p>Estos talles de <b>" + storeName + "</b> están en stock bajo:</p>"
                + "<table style=\"border-collapse:collapse;width:100%;\">"
                + "<tr style=\"background:#f3f4f6;\"><th style=\"text-align:left;padding:4px 8px;\">Producto</th>"
                + "<th style=\"text-align:left;padding:4px 8px;\">Talle</th>"
                + "<th style=\"padding:4px 8px;\">Stock</th></tr>"
                + rows
                + "</table></div>";

        try {
            var sender = mailSettingsService.buildSender();
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(mailSettingsService.get().getFromAddress());
            helper.setTo(toEmail);
            helper.setSubject("Stock bajo en " + storeName + " (" + items.size() + " talle(s))");
            helper.setText(html, true);
            sender.send(mime);
        } catch (Exception e) {
            log.error("No se pudo mandar la alerta de stock bajo: {}", e.getMessage());
        }
    }
}
