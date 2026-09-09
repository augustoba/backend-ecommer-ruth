package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del local editables desde el panel (nombre, WhatsApp, "sobre nosotros",
 * redes). Una sola fila (id fijo). Así el dueño/a puede cambiar el número de
 * WhatsApp o las redes sin redesplegar nada.
 */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
@NoArgsConstructor
public class SiteSettings {

    public static final String SINGLETON_ID = "config";

    @Id
    private String id = SINGLETON_ID;

    @Column(nullable = false)
    private String storeName;

    /** Número de WhatsApp en formato internacional sin +, espacios ni 15. */
    @Column(nullable = false)
    private String whatsappNumber;

    @Column(length = 2000)
    private String aboutText;

    /** Usuario de Instagram, sin @. */
    private String instagram;

    private String facebookUrl;

    /** Logo del negocio: URL o data URI. null = usar el logo por defecto (`logo.jpeg`). */
    @Column(length = 5_000_000)
    private String logoUrl;

    /**
     * Texto de saludo del mensaje de pedido de WhatsApp (antes del detalle).
     * Admite los tokens {tienda} y {codigo}. null = usar el texto por defecto.
     */
    @Column(length = 2000)
    private String whatsappIntro;

    /**
     * Texto de cierre del mensaje de pedido de WhatsApp (después de los totales),
     * ej: cómo coordinar el pago. Admite {tienda} y {codigo}. null = default.
     */
    @Column(length = 2000)
    private String whatsappClosing;

    /** Dirección del local (para la opción "Retiro en el local" del checkout). */
    @Column(length = 500)
    private String storeAddress;

    /**
     * Texto de la página "Cómo comprar" (texto libre, se respeta el salto de
     * línea). `length` grande → Hibernate lo mapea a MEDIUMTEXT (no entra en el
     * límite de tamaño de fila de MySQL como haría un VARCHAR grande).
     */
    @Column(length = 100_000)
    private String helpText;

    /**
     * Preguntas frecuentes, texto libre. Cada bloque separado por una línea en
     * blanco: la primera línea es la pregunta, el resto la respuesta. MEDIUMTEXT.
     */
    @Column(length = 500_000)
    private String faqText;

    // --- Medios de pago (aparece en el checkout si está habilitado Y tiene su dato) ---

    @Column(nullable = false)
    private boolean paymentTransferEnabled = false;
    /** Alias/CBU para transferencia. */
    @Column(length = 200)
    private String paymentTransferAlias;

    @Column(nullable = false)
    private boolean paymentQrTransferEnabled = false;
    /** Imagen (data URI) del QR de transferencia. */
    @Column(length = 5_000_000)
    private String paymentQrTransferImage;

    @Column(nullable = false)
    private boolean paymentQrCardEnabled = false;
    /** Imagen (data URI) del QR de pago con tarjeta. */
    @Column(length = 5_000_000)
    private String paymentQrCardImage;
    /** Link de cobro con tarjeta (ej. Mercado Pago). Alternativa/complemento al QR. */
    @Column(length = 1000)
    private String paymentCardLink;

    /** Habilita "efectivo al recibir/retirar". */
    @Column(nullable = false)
    private boolean paymentCashEnabled = false;
}
