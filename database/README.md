# Scripts de base de datos

Scripts SQL para crear y poblar la base **`estilos_pequenos`** (MySQL 8) sin
depender de que Hibernate arme el esquema solo.

| Archivo | Qué hace |
|---|---|
| `schema.sql` | Crea la base y **todas las tablas** (índices y FKs incluidos). |
| `seed.sql` | Carga la **config base**: parametrías, escalas de talle y descuentos por defecto. No carga productos. |
| `reset.sql` | Borra la base entera y la vuelve a crear vacía. |

## Uso

```bash
# desde la carpeta backend/
MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"   # o solo `mysql` si está en el PATH

# 1) crear tablas
"$MYSQL" -u root -p < database/schema.sql

# 2) cargar la config base
"$MYSQL" -u root -p estilos_pequenos < database/seed.sql

# reset completo (opcional)
"$MYSQL" -u root -p < database/reset.sql
```

En **MySQL Workbench**: abrir el `.sql`, y ejecutar con el rayo (⚡).

## ¿Y el `DataSeeder` de la app?

La app tiene un `DataSeeder` que, **solo si las tablas están vacías**, carga las
mismas parametrías/escalas/descuentos **+ 10 productos de ejemplo**. Sirve para
desarrollo (arrancás y ya tenés datos). Si ya corriste `seed.sql`, el
`DataSeeder` ve que hay datos y no hace nada — no se pisan.

Para apagar el seeder de la app: `SEED_ENABLED=false`.

## Relación con `ddl-auto`

| Modo (`application.yml`) | Cuándo |
|---|---|
| `update` (actual) | Desarrollo. Hibernate crea/ajusta las tablas al arrancar. Con esto **no hace falta correr `schema.sql`**. |
| `validate` | Producción / entornos controlados. Corrés `schema.sql` a mano y Hibernate solo verifica que coincida. |
| `none` | Hibernate no toca el esquema. Todo lo manejás con estos scripts. |

## Regenerar `schema.sql` desde el código

Cuando cambien las entidades JPA:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="\
  --spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=database/schema.gen.sql \
  --app.seed.enabled=false"
```

Genera `database/schema.gen.sql` (crudo, de Hibernate). Usalo como referencia
para actualizar `schema.sql` a mano (que está formateado y con nombres de
constraints legibles). `schema.gen.sql` está gitignored.
