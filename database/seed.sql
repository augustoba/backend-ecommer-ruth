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
--  Admin inicial (dueña de la tienda)
--    DNI: 11111111 · contraseña: ruth123
--    frase de recuperación: frase-de-recuperacion-cambiar
--  Los hash son BCrypt (cost 10). Cambiá contraseña y frase desde /admin/cuenta.
--  El rol se lo asigna la app en el primer arranque (DataSeeder / ensureInitialAdmin),
--  no hace falta cargarlo acá — pero necesita haber arrancado la app al menos una
--  vez antes para que exista el rol "Administrador".
-- ---------------------------------------------------------------------------
INSERT INTO admin_user (id, dni, nombre, apellido, email, password_hash, recovery_hash, enabled, created_at) VALUES
  ('seed-admin', '11111111', 'Ruth', 'Basaury', 'ruth@gmail.com',
   '$2a$10$ZFLQwovN0/tK/ii7RXNC4eM9BIQNgqFdSysjNaSM6pK4CRMXeOL/G',
   '$2a$10$.eOoZ23Uu4k.1DEj1NpOLekpl.PdGxt9NRMaxGN8J.DPQcIgBdbaW', 1, NOW(6))
ON DUPLICATE KEY UPDATE dni = dni;

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
