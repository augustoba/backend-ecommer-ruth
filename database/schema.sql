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
--  Usuario del panel de administración (contraseña hasheada con BCrypt)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id            VARCHAR(255) NOT NULL,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BIT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_user_username UNIQUE (username)
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
    image_url     MEDIUMTEXT     NOT NULL,   -- URL o data URI (imagen embebida)
    active        BIT            NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    size_scale_id VARCHAR(255),
    supplier_id   VARCHAR(255),
    cost_price    DECIMAL(12,2),
    PRIMARY KEY (id),
    KEY ix_product_active (active),
    KEY ix_product_supplier (supplier_id)
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
    kind             ENUM('MONTO','PARAMETRO') NOT NULL,
    discount_percent INTEGER      NOT NULL,
    enabled          BIT          NOT NULL,
    label            VARCHAR(255),
    min_amount       DECIMAL(38,2),          -- kind = MONTO
    group_id         VARCHAR(255),           -- kind = PARAMETRO
    option_id        VARCHAR(255),           -- kind = PARAMETRO
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS discount_config (
    id           VARCHAR(255) NOT NULL,      -- siempre 'config' (fila única)
    combine_mode ENUM('COMBINAR','MEJOR') NOT NULL,
    PRIMARY KEY (id)
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
    created_at       DATETIME(6)   NOT NULL,
    processed_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_number UNIQUE (number),
    KEY ix_orders_status (status)
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
