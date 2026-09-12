package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Configuración de las campañas automáticas de cupón por email (una sola fila,
 * como {@link SiteSettings}). Mientras `enabled` esté en false, el job diario
 * corre pero no manda nada (no-op seguro hasta cargar credenciales de mail reales).
 */
@Entity
@Table(name = "marketing_config")
public class MarketingConfig {

    public static final String SINGLETON_ID = "config";

    @Id
    private String id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean enabled = false;

    /** Porcentaje del cupón que se manda (1-100). */
    @Column(nullable = false)
    private int discountPercent = 10;

    /** Días sin comprar para entrar al segmento "inactivos". */
    @Column(nullable = false)
    private int inactivityDays = 45;

    /** Gasto acumulado (pedidos procesados) para entrar al segmento "VIP". */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal spendThreshold = new BigDecimal("200000");

    /** Tope de mails de campaña que se mandan por día. */
    @Column(nullable = false)
    private int dailyEmailCap = 250;

    /** Días de vigencia del cupón generado. */
    @Column(nullable = false)
    private int couponValidityDays = 30;

    /** Días sin volver a mandarle una campaña al mismo email, aunque siga calificando. */
    @Column(nullable = false)
    private int cooldownDays = 30;

    /**
     * Contenido del mail. Admiten los tokens {tienda}, {codigo}, {porcentaje} y
     * {vencimiento} (se reemplazan solos). null/blank = usar el texto por defecto.
     */
    @Column(length = 300)
    private String emailSubject;

    @Column(length = 4000)
    private String emailBody;

    /** Imagen que se muestra arriba del mail (data URI). null = sin imagen. */
    @Column(length = 5_000_000)
    private String emailImageUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getInactivityDays() {
        return inactivityDays;
    }

    public void setInactivityDays(int inactivityDays) {
        this.inactivityDays = inactivityDays;
    }

    public BigDecimal getSpendThreshold() {
        return spendThreshold;
    }

    public void setSpendThreshold(BigDecimal spendThreshold) {
        this.spendThreshold = spendThreshold;
    }

    public int getDailyEmailCap() {
        return dailyEmailCap;
    }

    public void setDailyEmailCap(int dailyEmailCap) {
        this.dailyEmailCap = dailyEmailCap;
    }

    public int getCouponValidityDays() {
        return couponValidityDays;
    }

    public void setCouponValidityDays(int couponValidityDays) {
        this.couponValidityDays = couponValidityDays;
    }

    public int getCooldownDays() {
        return cooldownDays;
    }

    public void setCooldownDays(int cooldownDays) {
        this.cooldownDays = cooldownDays;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public String getEmailImageUrl() {
        return emailImageUrl;
    }

    public void setEmailImageUrl(String emailImageUrl) {
        this.emailImageUrl = emailImageUrl;
    }
}
