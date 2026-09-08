# Scripts de base de datos

Scripts SQL para crear y poblar la base **`estilos_pequenos`** (MySQL 8).

| Archivo | Qué hace |
|---|---|
| **`setup.sql`** | **Todo junto:** crea la base, las 13 tablas y la config base. Es lo que se corre en un servidor nuevo al desplegar. |
| `schema.sql` | Solo la base + las tablas (sin datos). |
| `seed.sql` | Solo la config base: parametrías, escalas de talle, descuentos. Necesita la base ya creada. |
| `reset.sql` | Borra la base entera y la vuelve a crear vacía. |

`setup.sql` es la concatenación de `schema.sql` + `seed.sql`.

## Uso

```bash
MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"   # o solo `mysql` si está en el PATH

# instalación completa (una sola vez, en un server nuevo)
"$MYSQL" -u root -p < database/setup.sql

# --- o por partes ---
"$MYSQL" -u root -p < database/schema.sql                    # tablas
"$MYSQL" -u root -p estilos_pequenos < database/seed.sql     # config

# reset total
"$MYSQL" -u root -p < database/reset.sql
```

En **MySQL Workbench**: abrir el `.sql` y ejecutar con el rayo (⚡).

## Deploy

En el servidor:

1. `mysql < database/setup.sql` (crea todo).
2. Correr la app con `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (Hibernate solo
   verifica que el esquema coincida) — o `none`.
3. Los **productos** se cargan desde el panel de admin (o dejás que el
   `DataSeeder` de la app cargue los 10 de ejemplo la primera vez, si arrancás
   con `SEED_ENABLED=true` y la base vacía de productos).

> En **desarrollo local** no hace falta correr ningún SQL: la app usa
> `ddl-auto=update` + `createDatabaseIfNotExist=true` y crea todo sola.

## Mantener los scripts al día

- **`schema.sql`** se mantiene a mano en sincronía con las entidades JPA.
  Para regenerar la referencia cruda desde el código:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.arguments="\
    --spring.jpa.hibernate.ddl-auto=none \
    --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
    --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=database/schema.gen.sql \
    --app.seed.enabled=false"
  ```
  Genera `database/schema.gen.sql` (gitignored). Usalo para actualizar
  `schema.sql` a mano (formateado, con constraints con nombre legible).

- **`setup.sql`** se regenera concatenando:
  ```bash
  # (mantené el encabezado propio de setup.sql o volvé a ponerlo)
  cat database/schema.sql database/seed.sql > database/setup.sql
  ```
