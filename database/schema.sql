-- =============================================================================
--  Estilos Pequeños — esquema de la base de datos (MySQL 8)
-- =============================================================================
--  Genera todas las tablas. Podés correrlo a mano (MySQL Workbench o CLI) para
--  no depender de que Hibernate cree el esquema solo.
--
--  Uso:
--    mysql -u root -p < database/schema.sql
--
--  Este archivo se mantiene en sincronía con las entidades JPA. Para regenerarlo
--  a partir del código:
--    ./mvnw spring-boot:run \
--      -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=none \
--        --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
--        --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=database/schema.gen.sql \
--        --app.seed.enabled=false"
--
--  Si corrés la app con `spring.jpa.hibernate.ddl-auto=validate`, Hibernate
--  chequea que el esquema coincida con las entidades al arrancar.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS estilos_pequenos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE estilos_pequenos;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
--  Datos del local (una sola fila, editable desde el panel)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS site_settings (
    id               VARCHAR(255)  NOT NULL,   -- siempre 'config'
    store_name       VARCHAR(255)  NOT NULL,
    whatsapp_number  VARCHAR(255)  NOT NULL,
    about_text       VARCHAR(2000),
    about_page_enabled BIT NOT NULL DEFAULT 0,  -- página "Quiénes somos" publicada
    instagram        VARCHAR(255),
    facebook_url     VARCHAR(255),
    logo_url         MEDIUMTEXT,               -- logo del negocio (URL o data URI); null = logo.jpeg
    whatsapp_intro   VARCHAR(2000),            -- saludo del mensaje de pedido; null = texto por defecto
    whatsapp_closing VARCHAR(2000),            -- cierre del mensaje de pedido; null = texto por defecto
    store_address    VARCHAR(500),             -- dirección del local (opción "retiro")
    store_photo_url  MEDIUMTEXT,               -- foto del local (URL o data URI)
    help_text        MEDIUMTEXT,               -- pagina "como comprar" (texto libre)
    faq_text         MEDIUMTEXT,               -- preguntas frecuentes (bloques separados por linea en blanco)
    payment_transfer_enabled     BIT NOT NULL DEFAULT 0,
    payment_transfer_alias       VARCHAR(200),
    payment_qr_transfer_enabled  BIT NOT NULL DEFAULT 0,
    payment_qr_transfer_image    MEDIUMTEXT,
    payment_qr_card_enabled      BIT NOT NULL DEFAULT 0,
    payment_qr_card_image        MEDIUMTEXT,
    payment_card_link            VARCHAR(1000),
    payment_cash_enabled         BIT NOT NULL DEFAULT 0,
    mp_enabled                   BIT NOT NULL DEFAULT 0,   -- Mercado Pago activado por el dueño
    mp_access_token              VARCHAR(300),             -- secreto: nunca se devuelve en ninguna respuesta
    mp_public_key                VARCHAR(300),
    low_stock_alert_enabled      BIT NOT NULL DEFAULT 0,   -- mail diario de talles por reponer
    low_stock_alert_email        VARCHAR(300),
    cloudinary_cloud_name        VARCHAR(200),             -- cuenta de Cloudinary (subida de fotos)
    cloudinary_upload_preset     VARCHAR(200),
    -- Scaffolding de SMTP del panel /admin/superadmin/mail. OJO: NO es la config que
    -- manda mail de verdad — la real vive en platform_mail_settings (ver PROYECTO.md #51).
    smtp_host                    VARCHAR(300),
    smtp_port                    INTEGER,
    smtp_username                VARCHAR(300),
    smtp_password                VARCHAR(500),             -- secreto: nunca se devuelve
    smtp_from_email              VARCHAR(300),
    smtp_from_name               VARCHAR(200),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Usuario del panel de administración (contraseña hasheada con BCrypt)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `role` (
    id         VARCHAR(255) NOT NULL,
    name       VARCHAR(60)  NOT NULL,
    `system`   BIT          NOT NULL DEFAULT 0,   -- rol "Superadmin": todos los permisos, no editable
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permission (
    role_id    VARCHAR(255) NOT NULL,
    permission ENUM(
        'CAROUSEL_MANAGE','CASH_REGISTER_VIEW','COUPONS_MANAGE','DISCOUNTS_MANAGE','EXCHANGES_USE',
        'EXPENSES_MANAGE','FINANCE_VIEW','MARKETING_MANAGE','METRICS_VIEW','ORDERS_MANAGE','ORDERS_VIEW',
        'PARAMS_MANAGE','PAYMENTS_MANAGE','PLATFORM_SETTINGS_MANAGE','POS_USE','PRODUCTS_MANAGE',
        'PRODUCTS_VIEW','SHIFTS_MANAGE','SIZE_SCALES_MANAGE','STOCK_MOVEMENTS_VIEW','SUPPLIERS_MANAGE',
        'USERS_MANAGE'
    ) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES `role` (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS admin_user (
    id            VARCHAR(255) NOT NULL,
    dni           VARCHAR(20)  NOT NULL,      -- identificador de login (reemplaza al username viejo)
    nombre        VARCHAR(255) NOT NULL,
    apellido      VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BIT          NOT NULL,
    super_admin   BIT          NOT NULL DEFAULT 0, -- acceso a Cloudinary/mail, aparte del rol (ver AdminUser.superAdmin)
    role_id       VARCHAR(255),                -- rol (define los permisos)
    created_at    DATETIME(6)  NOT NULL,
    reset_token_hash       VARCHAR(255),        -- hash del link de "olvidé mi contraseña" pendiente (null = ninguno)
    reset_token_expires_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_user_dni UNIQUE (dni),
    CONSTRAINT uk_admin_user_email UNIQUE (email),
    CONSTRAINT fk_admin_user_role FOREIGN KEY (role_id) REFERENCES `role` (id)
) ENGINE=InnoDB;

-- Credenciales del servicio de mail (SMTP), fila única. Sólo editable por el superadmin.
CREATE TABLE IF NOT EXISTS platform_mail_settings (
    id           VARCHAR(255) NOT NULL,  -- siempre 'config'
    host         VARCHAR(255) NOT NULL,
    port         INT          NOT NULL,
    username     VARCHAR(255) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Parametrías (clasificación de prendas: público, tipo, estación…)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS param_group (
    id              VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    multiple        BIT          NOT NULL,
    show_in_catalog BIT          NOT NULL,
    `system`        BIT          NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS param_option (
    id       VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    label    VARCHAR(255) NOT NULL,
    position INTEGER      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_param_option_group
        FOREIGN KEY (group_id) REFERENCES param_group (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Escalas de talle (ropa bebé / niños / adultos, calzado…)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS size_scale (
    id         VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    `system`   BIT          NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS size_scale_value (
    scale_id   VARCHAR(255) NOT NULL,
    idx        INTEGER      NOT NULL,
    size_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (scale_id, idx),
    CONSTRAINT fk_size_scale_value_scale
        FOREIGN KEY (scale_id) REFERENCES size_scale (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Proveedores (info interna del admin)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supplier (
    id         VARCHAR(255)  NOT NULL,
    name       VARCHAR(255)  NOT NULL,
    phone      VARCHAR(255),
    address    VARCHAR(255),
    notes      VARCHAR(2000),
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Productos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product (
    id            VARCHAR(255)   NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    description   VARCHAR(4000)  NOT NULL,
    price         DECIMAL(12,2)  NOT NULL,
    age_range     VARCHAR(255)   NOT NULL,
    video_url     VARCHAR(500),                        -- link a un video de la prenda (YouTube), opcional
    active        BIT            NOT NULL,
    discontinued  BIT            NOT NULL DEFAULT 0,   -- "no reponer": sale de las alertas de stock bajo
    deleted       BIT            NOT NULL DEFAULT 0,   -- soft-delete: archivado, sale de catalogo y listados
    created_at    DATETIME(6)    NOT NULL,
    size_scale_id VARCHAR(255),
    supplier_id   VARCHAR(255),
    cost_price    DECIMAL(12,2),
    low_stock_threshold INTEGER,               -- umbral de stock bajo propio (null = default global)
    barcode       VARCHAR(64),                 -- código de barras interno u original, opcional
    PRIMARY KEY (id),
    KEY ix_product_active (active),
    KEY ix_product_supplier (supplier_id),
    UNIQUE KEY uq_product_barcode (barcode)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_image (
    product_id VARCHAR(255) NOT NULL,
    idx        INTEGER      NOT NULL,
    url        MEDIUMTEXT   NOT NULL,   -- URL o data URI (imagen embebida). idx 0 = portada
    PRIMARY KEY (product_id, idx),
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_param (
    product_id VARCHAR(255) NOT NULL,
    group_id   VARCHAR(255) NOT NULL,
    option_id  VARCHAR(255) NOT NULL,
    PRIMARY KEY (product_id, group_id, option_id),
    CONSTRAINT fk_product_param_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_size_stock (
    product_id VARCHAR(255) NOT NULL,
    idx        INTEGER      NOT NULL,
    size_value VARCHAR(255) NOT NULL,
    stock      INTEGER      NOT NULL,
    PRIMARY KEY (product_id, idx),
    CONSTRAINT fk_product_size_stock_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Descuentos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount (
    id               VARCHAR(255) NOT NULL,
    kind             ENUM('ENVIO_GRATIS','MONTO','PAGO','PARAMETRO') NOT NULL,
    discount_percent INTEGER      NOT NULL,
    enabled          BIT          NOT NULL,
    stackable        BIT          NOT NULL DEFAULT 0,   -- acumulable con otros descuentos
    label            VARCHAR(255),
    detail           VARCHAR(300),           -- letra chica configurable (ej: "solo microcentro")
    starts_at        DATE,                   -- vigencia opcional (inclusive)
    ends_at          DATE,                   -- vigencia opcional (inclusive)
    min_amount       DECIMAL(38,2),          -- kind = MONTO o ENVIO_GRATIS
    group_id         VARCHAR(255),           -- kind = PARAMETRO
    option_id        VARCHAR(255),           -- kind = PARAMETRO
    payment_methods  VARCHAR(100),           -- kind = PAGO ("TRANSFER,CASH")
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Cupones (codigos que el cliente escribe en el carrito)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupon (
    id          VARCHAR(255)  NOT NULL,
    code        VARCHAR(40)   NOT NULL,
    kind        ENUM('AMOUNT','PERCENT') NOT NULL,
    value       DECIMAL(12,2) NOT NULL,
    min_amount  DECIMAL(12,2),
    max_uses    INTEGER,                    -- null = ilimitado
    used_count  INTEGER       NOT NULL DEFAULT 0,
    expires_at  DATE,
    enabled     BIT           NOT NULL DEFAULT 1,
    stackable   BIT           NOT NULL DEFAULT 1,   -- combina con los descuentos automaticos
    label       VARCHAR(200),
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupon_code UNIQUE (code)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Pedidos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id               VARCHAR(255)  NOT NULL,
    number           BIGINT        NOT NULL,   -- correlativo → code "PED-0001"
    customer_name    VARCHAR(255)  NOT NULL,
    customer_email   VARCHAR(200),             -- opcional, no bloquea la venta (campañas / base de clientes)
    subtotal         DECIMAL(12,2) NOT NULL,
    discount_percent INTEGER       NOT NULL,
    discount_amount  DECIMAL(12,2) NOT NULL,
    total            DECIMAL(12,2) NOT NULL,
    status           ENUM('CANCELADO','PENDIENTE','PROCESADO') NOT NULL,
    channel          ENUM('LOCAL','WEB') NOT NULL DEFAULT 'WEB',
    delivery_method  ENUM('PICKUP','SHIPPING') NOT NULL DEFAULT 'PICKUP',
    shipping_address    VARCHAR(500),
    shipping_reference  VARCHAR(500),
    shipping_lat     DOUBLE,
    shipping_lng     DOUBLE,
    payment_method   ENUM('CASH','MERCADOPAGO','QR_CARD','QR_TRANSFER','TRANSFER'),
    payment_status   ENUM('APPROVED','PENDING','REJECTED'),  -- solo para payment_method=MERCADOPAGO
    mp_preference_id VARCHAR(100),              -- id de la preferencia creada en Mercado Pago
    mp_checkout_url  VARCHAR(500),              -- init_point devuelto al crear la preferencia
    mp_payment_id    VARCHAR(100),              -- id del pago aprobado, una vez confirmado por webhook
    free_shipping_note VARCHAR(300),
    discount_note      VARCHAR(500),
    coupon_code       VARCHAR(40),              -- cupon aplicado (null = ninguno)
    coupon_discount   DECIMAL(12,2),            -- descuento en pesos del cupon (aparte del automatico)
    created_at       DATETIME(6)   NOT NULL,
    processed_at     DATETIME(6),
    created_by_dni    VARCHAR(20),   -- quien armo el pedido (null = checkout web)
    created_by_name   VARCHAR(200),
    confirmed_by_dni  VARCHAR(20),   -- quien lo confirmo/cobro
    confirmed_by_name VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_number UNIQUE (number),
    KEY ix_orders_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Cambios de prenda en el local
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exchange (
    id             VARCHAR(255)  NOT NULL,
    number         BIGINT        NOT NULL,   -- correlativo → code "CAM-0001"
    customer_name  VARCHAR(255)  NOT NULL,
    returned_total DECIMAL(12,2) NOT NULL,
    taken_total    DECIMAL(12,2) NOT NULL,
    difference     DECIMAL(12,2) NOT NULL,   -- taken - returned (+ cobra el local / - a favor del cliente)
    payment_method ENUM('CASH','MERCADOPAGO','QR_CARD','QR_TRANSFER','TRANSFER'),
    note           VARCHAR(500),
    created_at     DATETIME(6)   NOT NULL,
    processed_by_dni  VARCHAR(20),
    processed_by_name VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT uk_exchange_number UNIQUE (number)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Turnos (abrir/cerrar) de vendedores/cajeros
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shift (
    id         VARCHAR(255) NOT NULL,
    user_dni   VARCHAR(20)  NOT NULL,
    user_name  VARCHAR(200) NOT NULL,
    opened_at  DATETIME(6)  NOT NULL,
    closed_at  DATETIME(6),           -- null = turno abierto
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exchange_line (
    id           VARCHAR(255)  NOT NULL,
    exchange_id  VARCHAR(255)  NOT NULL,
    idx          INTEGER,
    kind         ENUM('DEVUELTA','LLEVADA') NOT NULL,
    product_id   VARCHAR(255)  NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    size_value   VARCHAR(255)  NOT NULL,
    quantity     INTEGER       NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_exchange_line_exchange FOREIGN KEY (exchange_id) REFERENCES exchange (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_line (
    id           VARCHAR(255)  NOT NULL,
    order_id     VARCHAR(255)  NOT NULL,
    idx          INTEGER,
    product_id   VARCHAR(255)  NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    size_value   VARCHAR(255)  NOT NULL,
    quantity     INTEGER       NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    cost_price   DECIMAL(12,2),            -- costo del producto congelado al confirmar el pedido (null = sin dato)
    accepted     BIT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_line_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    KEY ix_order_line_product (product_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Carrusel de la home
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hero_slide (
    id        VARCHAR(255) NOT NULL,
    image_url MEDIUMTEXT   NOT NULL,
    alt       VARCHAR(255) NOT NULL,
    position  INTEGER      NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Costeo: historial de movimientos de stock
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_movement (
    id               VARCHAR(255)  NOT NULL,
    product_id       VARCHAR(255)  NOT NULL,
    product_name     VARCHAR(255)  NOT NULL,
    size_value       VARCHAR(255)  NOT NULL,
    quantity_delta   INTEGER       NOT NULL,   -- positivo = entro, negativo = salio
    reason           ENUM('AJUSTE_MANUAL','ALTA_INICIAL','CAMBIO_DEVUELTA','CAMBIO_LLEVADA','ENTRADA_COMPRA','VENTA') NOT NULL,
    note             VARCHAR(500),
    reference_id     VARCHAR(255),             -- orderId/exchangeId/supplierId segun el motivo
    unit_cost        DECIMAL(12,2),            -- solo ENTRADA_COMPRA
    created_by_dni   VARCHAR(255),
    created_by_name  VARCHAR(255),
    created_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY ix_stock_movement_product (product_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Gastos y presupuesto (Balance)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expense (
    id                  VARCHAR(255)  NOT NULL,
    expense_date        DATE          NOT NULL,
    category_option_id  VARCHAR(255),
    amount              DECIMAL(12,2) NOT NULL,
    description         VARCHAR(2000),
    repeat_monthly      BIT           NOT NULL DEFAULT 0,
    recurring_group_id  VARCHAR(255),
    created_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS expense_budget (
    id                  VARCHAR(255)  NOT NULL,
    category_option_id  VARCHAR(255)  NOT NULL,
    monthly_amount      DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_expense_budget_category (category_option_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Campañas de marketing por email (cupón automático a inactivos / VIP)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS marketing_config (
    id                   VARCHAR(255)  NOT NULL,   -- siempre 'config'
    enabled              BIT           NOT NULL DEFAULT 0,  -- false = el job corre pero no manda nada
    discount_percent     INTEGER       NOT NULL,   -- % del cupón que se manda
    inactivity_days      INTEGER       NOT NULL,   -- días sin comprar para ser "inactivo"
    spend_threshold      DECIMAL(12,2) NOT NULL,   -- gasto acumulado para ser "VIP"
    daily_email_cap      INTEGER       NOT NULL,   -- tope de mails de campaña por día
    coupon_validity_days INTEGER       NOT NULL,   -- vigencia del cupón generado
    cooldown_days        INTEGER       NOT NULL,   -- no repetirle al mismo email antes de N días
    email_subject        VARCHAR(300),             -- admiten tokens {tienda}/{codigo}/{porcentaje}/{vencimiento}
    email_body           VARCHAR(4000),
    email_image_url      MEDIUMTEXT,               -- imagen arriba del mail (URL o data URI)
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS marketing_send (
    id                      VARCHAR(255)  NOT NULL,
    email                   VARCHAR(200)  NOT NULL,
    reason                  ENUM('INACTIVE','VIP') NOT NULL,
    coupon_code             VARCHAR(40),             -- null = falló antes de crear el cupón
    sent_at                 DATETIME(6)   NOT NULL,
    status                  ENUM('FAILED','SENT') NOT NULL,
    error_message           VARCHAR(500),
    lifetime_spend_snapshot DECIMAL(12,2),           -- snapshot al momento del envío
    last_order_at_snapshot  DATETIME(6),
    PRIMARY KEY (id),
    KEY ix_marketing_send_sent_at (sent_at)          -- historial (orden), tope diario y cooldown
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
