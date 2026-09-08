-- =============================================================================
--  Estilos Pequeños — reset total de la base
-- =============================================================================
--  Borra la base entera y la vuelve a crear vacía. Después corré schema.sql
--  y (opcional) seed.sql.
--
--    mysql -u root -p < database/reset.sql
--    mysql -u root -p < database/schema.sql
--    mysql -u root -p estilos_pequenos < database/seed.sql
-- =============================================================================

DROP DATABASE IF EXISTS estilos_pequenos;
CREATE DATABASE estilos_pequenos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
