-- =============================================================================
--  Estilos Pequeños — instalación completa de la base (MySQL 8)
-- =============================================================================
--  UN SOLO script: crea la base `estilos_pequenos`, todas las tablas y la
--  config base (admin, parametrías, escalas de talle, descuentos). Es lo que se
--  corre en un servidor nuevo al desplegar.
--
--    mysql -u root -p < database/setup.sql
--
--  (o abrirlo en MySQL Workbench y ejecutarlo con el rayo ⚡)
--
--  Admin inicial:  usuario `admin`  /  contraseña `ruth123`
--                  frase de recuperación `frase-de-recuperacion-cambiar`
--                  (todo cambiable desde /admin/cuenta)
--
--  Es la concatenación de `schema.sql` + `seed.sql`. Si editás alguno de esos,
--  regenerá este archivo:
--    cat database/schema.sql database/seed.sql > database/setup.sql
--
--  Después: correr la app con `spring.jpa.hibernate.ddl-auto=validate` (o `none`).

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
    id              VARCHAR(255)  NOT NULL,   -- siempre 'config'
    store_name      VARCHAR(255)  NOT NULL,
    whatsapp_number VARCHAR(255)  NOT NULL,
    about_text      VARCHAR(2000),
    instagram       VARCHAR(255),
    facebook_url    VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
--  Usuario del panel de administración (contraseña hasheada con BCrypt)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id            VARCHAR(255) NOT NULL,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    recovery_hash VARCHAR(255),                -- frase de recuperación (BCrypt)
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
    active        BIT            NOT NULL,
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


-- =============================================================================
--  Estilos Pequeños — datos base (config que necesita cualquier entorno)
-- =============================================================================
--  Carga las parametrías, escalas de talle y descuentos por defecto.
--  NO carga productos: el catálogo real se carga desde el panel de admin
--  (en desarrollo, el `DataSeeder` de la app siembra 10 productos de ejemplo
--  la primera vez que arranca con la base vacía).
--
--  Uso:
--    mysql -u root -p estilos_pequenos < database/seed.sql
--
--  Es idempotente: usa INSERT ... ON DUPLICATE KEY UPDATE, se puede correr
--  varias veces sin romper nada.
-- =============================================================================

USE estilos_pequenos;

-- ---------------------------------------------------------------------------
--  Datos del local (editables desde /admin/ajustes)
-- ---------------------------------------------------------------------------
INSERT INTO site_settings (id, store_name, whatsapp_number, about_text, instagram, facebook_url) VALUES
  ('config', 'Estilos Pequeños', '5491122334455',
   'Somos Estilos Pequeños 🧸 Hace 5 años vestimos a los más chicos con ropa cómoda, de calidad y con onda. Elegimos cada prenda pensando en la comodidad de los peques y la tranquilidad de las familias. ¡Gracias por elegirnos!',
   'estilospequenos_', 'https://www.facebook.com/share/1NZXdYgick/')
ON DUPLICATE KEY UPDATE id = id;

-- ---------------------------------------------------------------------------
--  Admin inicial
--    usuario: admin
--    contraseña: ruth123
--    frase de recuperación: frase-de-recuperacion-cambiar
--  Los hash son BCrypt (cost 10). Cambiá contraseña y frase desde /admin/cuenta.
-- ---------------------------------------------------------------------------
INSERT INTO admin_user (id, username, password_hash, recovery_hash, enabled, created_at) VALUES
  ('seed-admin', 'admin',
   '$2a$10$ZFLQwovN0/tK/ii7RXNC4eM9BIQNgqFdSysjNaSM6pK4CRMXeOL/G',
   '$2a$10$.eOoZ23Uu4k.1DEj1NpOLekpl.PdGxt9NRMaxGN8J.DPQcIgBdbaW', 1, NOW(6))
ON DUPLICATE KEY UPDATE username = username;

-- ---------------------------------------------------------------------------
--  Parametrías
-- ---------------------------------------------------------------------------
INSERT INTO param_group (id, name, multiple, show_in_catalog, `system`, created_at) VALUES
  ('grp-publico',  'Público',        0, 1, 1, NOW(6)),
  ('grp-tipo',     'Tipo de prenda', 0, 1, 0, NOW(6)),
  ('grp-estacion', 'Estación',       1, 1, 0, NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO param_option (id, group_id, label, position) VALUES
  ('publico-bebe',   'grp-publico', 'Bebé',   0),
  ('publico-nena',   'grp-publico', 'Nena',   1),
  ('publico-nene',   'grp-publico', 'Nene',   2),
  ('publico-unisex', 'grp-publico', 'Unisex', 3),

  ('tipo-remera',    'grp-tipo', 'Remera',            0),
  ('tipo-buzo',      'grp-tipo', 'Buzo / Campera',    1),
  ('tipo-pantalon',  'grp-tipo', 'Pantalón',          2),
  ('tipo-jean',      'grp-tipo', 'Jean',              3),
  ('tipo-vestido',   'grp-tipo', 'Vestido / Pollera', 4),
  ('tipo-body',      'grp-tipo', 'Body / Enterito',   5),
  ('tipo-conjunto',  'grp-tipo', 'Conjunto',          6),
  ('tipo-calzado',   'grp-tipo', 'Calzado',           7),
  ('tipo-accesorio', 'grp-tipo', 'Accesorio',         8),

  ('estacion-primavera', 'grp-estacion', 'Primavera',   0),
  ('estacion-verano',    'grp-estacion', 'Verano',      1),
  ('estacion-otono',     'grp-estacion', 'Otoño',       2),
  ('estacion-invierno',  'grp-estacion', 'Invierno',    3),
  ('estacion-todo',      'grp-estacion', 'Todo el año', 4)
ON DUPLICATE KEY UPDATE label = VALUES(label), position = VALUES(position);

-- ---------------------------------------------------------------------------
--  Escalas de talle
-- ---------------------------------------------------------------------------
INSERT INTO size_scale (id, name, `system`, created_at) VALUES
  ('escala-bebe',            'Ropa bebé (por edad)', 1, NOW(6)),
  ('escala-ninos',           'Ropa niños',           1, NOW(6)),
  ('escala-adultos',         'Ropa adultos',         1, NOW(6)),
  ('escala-calzado-ninos',   'Calzado niños',        1, NOW(6)),
  ('escala-calzado-adultos', 'Calzado adultos',      1, NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

DELETE FROM size_scale_value WHERE scale_id IN
  ('escala-bebe','escala-ninos','escala-adultos','escala-calzado-ninos','escala-calzado-adultos');

INSERT INTO size_scale_value (scale_id, idx, size_value) VALUES
  ('escala-bebe', 0, 'RN'), ('escala-bebe', 1, '0-3M'), ('escala-bebe', 2, '3-6M'),
  ('escala-bebe', 3, '6-12M'), ('escala-bebe', 4, '12-18M'), ('escala-bebe', 5, '18-24M'),
  ('escala-bebe', 6, '24M'),

  ('escala-ninos', 0, '1'), ('escala-ninos', 1, '2'), ('escala-ninos', 2, '3'),
  ('escala-ninos', 3, '4'), ('escala-ninos', 4, '6'), ('escala-ninos', 5, '8'),
  ('escala-ninos', 6, '10'), ('escala-ninos', 7, '12'), ('escala-ninos', 8, '14'),
  ('escala-ninos', 9, '16'),

  ('escala-adultos', 0, 'XS'), ('escala-adultos', 1, 'S'), ('escala-adultos', 2, 'M'),
  ('escala-adultos', 3, 'L'), ('escala-adultos', 4, 'XL'), ('escala-adultos', 5, 'XXL'),

  ('escala-calzado-ninos', 0, '17'),  ('escala-calzado-ninos', 1, '18'),  ('escala-calzado-ninos', 2, '19'),
  ('escala-calzado-ninos', 3, '20'),  ('escala-calzado-ninos', 4, '21'),  ('escala-calzado-ninos', 5, '22'),
  ('escala-calzado-ninos', 6, '23'),  ('escala-calzado-ninos', 7, '24'),  ('escala-calzado-ninos', 8, '25'),
  ('escala-calzado-ninos', 9, '26'),  ('escala-calzado-ninos', 10, '27'), ('escala-calzado-ninos', 11, '28'),
  ('escala-calzado-ninos', 12, '29'), ('escala-calzado-ninos', 13, '30'), ('escala-calzado-ninos', 14, '31'),
  ('escala-calzado-ninos', 15, '32'), ('escala-calzado-ninos', 16, '33'), ('escala-calzado-ninos', 17, '34'),

  ('escala-calzado-adultos', 0, '34'),  ('escala-calzado-adultos', 1, '35'),  ('escala-calzado-adultos', 2, '36'),
  ('escala-calzado-adultos', 3, '37'),  ('escala-calzado-adultos', 4, '38'),  ('escala-calzado-adultos', 5, '39'),
  ('escala-calzado-adultos', 6, '40'),  ('escala-calzado-adultos', 7, '41'),  ('escala-calzado-adultos', 8, '42'),
  ('escala-calzado-adultos', 9, '43'),  ('escala-calzado-adultos', 10, '44'), ('escala-calzado-adultos', 11, '45'),
  ('escala-calzado-adultos', 12, '46');

-- ---------------------------------------------------------------------------
--  Descuentos por defecto
-- ---------------------------------------------------------------------------
INSERT INTO discount (id, kind, discount_percent, enabled, label, min_amount, group_id, option_id) VALUES
  ('seed-monto-100k', 'MONTO', 20, 1, NULL, 100000.00, NULL, NULL),
  ('seed-monto-200k', 'MONTO', 25, 1, NULL, 200000.00, NULL, NULL)
ON DUPLICATE KEY UPDATE discount_percent = VALUES(discount_percent);

INSERT INTO discount_config (id, combine_mode) VALUES ('config', 'MEJOR')
ON DUPLICATE KEY UPDATE combine_mode = combine_mode;
