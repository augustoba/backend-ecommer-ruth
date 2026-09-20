-- =============================================================================
--  Estilos Pequeños — borra los datos de DEMO (dev)
-- =============================================================================
--  Deshace lo que cargó `DemoDataSeeder` (backend, `SEED_DEMO_ENABLED=true`).
--  Los pedidos de demo se identifican por el mail del cliente: terminan en
--  `@demo.local`.
--
--  Uso:
--    mysql -u root -p estilos_pequenos < database/reset-demo.sql
--
--  ⚠️  SÓLO PARA DESARROLLO. Qué borra y qué no:
--    - Borra los pedidos de demo (y sus líneas, por FK) y sus movimientos de
--      stock, más los movimientos de las compras de reposición del seeder.
--    - Borra TODOS los gastos y presupuestos, TODOS los cambios de prenda,
--      TODOS los turnos y las campañas enviadas a mails @demo.local. El seeder
--      los crea sin un marcador propio, así que la única forma de sacarlos es
--      borrarlos enteros: si cargaste gastos/cambios/turnos REALES en la misma
--      base, este script te los va a borrar también. En una base de desarrollo
--      usada sólo con datos de demo, no hay problema.
--    - NO borra productos, parametrías, escalas, descuentos, el sitio
--      (`site_settings`) ni los usuarios del panel.
--    - Sí borra los proveedores duplicados que hubieran quedado de una corrida
--      interrumpida, dejando los `demo-prov-*`, que son los que referencian los
--      productos.
-- =============================================================================

USE estilos_pequenos;

-- 1) Movimientos de stock de los pedidos de demo y de las compras de reposición.
--    (Antes de borrar los pedidos, porque la subconsulta los necesita.)
DELETE FROM stock_movement
WHERE reference_id IN (SELECT id FROM orders WHERE customer_email LIKE '%@demo.local')
   OR (reason = 'ENTRADA_COMPRA' AND reference_id NOT LIKE 'demo-prov-%');

-- 2) Pedidos de demo. Las líneas se borran explícitamente y no se confía en el
--    ON DELETE CASCADE: la FK que crea Hibernate con `ddl-auto=update` NO lo
--    tiene (aunque sí esté en `schema.sql`), así que en una base de dev el
--    borrado del padre falla. Así funciona en las dos.
DELETE FROM order_line
WHERE order_id IN (SELECT id FROM orders WHERE customer_email LIKE '%@demo.local');
DELETE FROM orders WHERE customer_email LIKE '%@demo.local';

-- 3) Campañas de marketing (historial + cooldown).
DELETE FROM marketing_send WHERE email LIKE '%@demo.local';

-- 4) Cambios de prenda (mismo caso que order_line: la FK no tiene cascade).
DELETE FROM exchange_line;
DELETE FROM exchange;

-- 5) Gastos y presupuestos.
DELETE FROM expense_budget;
DELETE FROM expense;

-- 6) Turnos.
DELETE FROM shift;

-- 7) Cupones de demo (los dos fijos y el lote de campaña).
DELETE FROM coupon WHERE code IN ('BIENVENIDA10', 'VERANO25') OR code LIKE 'DEMO%';

-- 8) Proveedores duplicados de una corrida interrumpida: se dejan los
--    `demo-prov-*` (los que referencian los productos y los movimientos).
DELETE FROM supplier WHERE id NOT LIKE 'demo-prov-%';

-- Resumen de lo que queda.
SELECT 'orders' AS tabla, COUNT(*) AS filas FROM orders
UNION ALL SELECT 'order_line', COUNT(*) FROM order_line
UNION ALL SELECT 'stock_movement', COUNT(*) FROM stock_movement
UNION ALL SELECT 'supplier', COUNT(*) FROM supplier
UNION ALL SELECT 'expense', COUNT(*) FROM expense
UNION ALL SELECT 'exchange', COUNT(*) FROM exchange
UNION ALL SELECT 'shift', COUNT(*) FROM shift
UNION ALL SELECT 'coupon', COUNT(*) FROM coupon
UNION ALL SELECT 'marketing_send', COUNT(*) FROM marketing_send;
