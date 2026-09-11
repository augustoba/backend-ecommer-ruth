package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Configuración de las campañas automáticas de cupón por email (una sola fila,
 * como {@link SiteSettings}). Mientras `enabled` esté en false, el job diario
 * corre pero no manda nada (no-op seguro hasta cargar credenciales de mail reales).
 */
@Entity
@Table(name = "marketing_config")
@Getter
@Setter
@NoArgsConstructor
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
}
