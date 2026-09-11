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
    instagram        VARCHAR(255),
    facebook_url     VARCHAR(255),
    logo_url         MEDIUMTEXT,               -- logo del negocio (URL o data URI); null = logo.jpeg
    whatsapp_intro   VARCHAR(2000),            -- saludo del mensaje de pedido; null = texto por defecto
    whatsapp_closing VARCHAR(2000),            -- cierre del mensaje de pedido; null = texto por defecto
    store_address    VARCHAR(500),             -- dirección del local (opción "retiro")
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
    permission VARCHAR(40)  NOT NULL,
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
    recovery_hash VARCHAR(255),                -- frase de recuperación (BCrypt)
    enabled       BIT          NOT NULL,
    role_id       VARCHAR(255),                -- rol (define los permisos)
    created_at    DATETIME(6)  NOT NULL,
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
    active        BIT            NOT NULL,
    discontinued  BIT            NOT NULL DEFAULT 0,   -- "no reponer": sale de las alertas de stock bajo
    deleted       BIT            NOT NULL DEFAULT 0,   -- soft-delete: archivado, sale de catalogo y listados
    created_at    DATETIME(6)    NOT NULL,
    size_scale_id VARCHAR(255),
    supplier_id   VARCHAR(255),
    cost_price    DECIMAL(12,2),
    low_stock_threshold INTEGER,               -- umbral de stock bajo propio (null = default global)
    PRIMARY KEY (id),
    KEY ix_product_active (active),
    KEY ix_product_supplier (supplier_id)
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
    kind             ENUM('MONTO','PARAMETRO','PAGO','ENVIO_GRATIS') NOT NULL,
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
    kind        ENUM('PERCENT','AMOUNT') NOT NULL,
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
    subtotal         DECIMAL(12,2) NOT NULL,
    discount_percent INTEGER       NOT NULL,
    discount_amount  DECIMAL(12,2) NOT NULL,
    total            DECIMAL(12,2) NOT NULL,
    status           ENUM('CANCELADO','PENDIENTE','PROCESADO') NOT NULL,
    channel          ENUM('WEB','LOCAL') NOT NULL DEFAULT 'WEB',
    delivery_method  ENUM('PICKUP','SHIPPING') NOT NULL DEFAULT 'PICKUP',
    shipping_address    VARCHAR(500),
    shipping_reference  VARCHAR(500),
    shipping_lat     DOUBLE,
    shipping_lng     DOUBLE,
    payment_method   ENUM('TRANSFER','QR_TRANSFER','QR_CARD','CASH'),
    free_shipping_note VARCHAR(300),
    discount_note      VARCHAR(500),
    coupon_code       VARCHAR(40),              -- cupon aplicado (null = ninguno)
    coupon_discount   DECIMAL(12,2),            -- descuento en pesos del cupon (aparte del automatico)
    created_at       DATETIME(6)   NOT NULL,
    processed_at     DATETIME(6),
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
    payment_method ENUM('TRANSFER','QR_TRANSFER','QR_CARD','CASH'),
    note           VARCHAR(500),
    created_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_exchange_number UNIQUE (number)
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

SET FOREIGN_KEY_CHECKS = 1;
