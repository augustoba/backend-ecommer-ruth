# Estilos Pequeños — Backend

API REST del catálogo público y del panel de administración de **Estilos Pequeños**.

- **Java 21** + **Spring Boot 3.3** + **MySQL 8**
- Autenticación **JWT** para los endpoints del admin (`/api/admin/**`)
- Docs interactivas: **Swagger UI** en `http://localhost:8080/swagger-ui.html`

> El frontend Angular **todavía no está conectado** a este backend (sigue con
> `localStorage`). Conectarlo es un paso aparte.

## Requisitos

- JDK 21
- MySQL 8 corriendo en `localhost:3306` (el service `MySQL80` en esta PC).
  La base `estilos_pequenos` se crea sola (`createDatabaseIfNotExist=true`).
- No hace falta Maven instalado: usá el wrapper `./mvnw`.

## Configurar credenciales

El backend necesita el usuario/clave de MySQL. Dos opciones:

**A) Variables de entorno** (recomendado):
```bash
export DB_USER=root
export DB_PASSWORD=tu_password
export JWT_SECRET=un-secreto-largo-de-al-menos-32-caracteres
```

**B) Archivo local** (gitignored): copiá
`src/main/resources/application-local.yml.example` a
`src/main/resources/application-local.yml`, completá los valores, y corré con
`-Dspring-boot.run.profiles=local`.

Credenciales del admin (para el login): por defecto `admin` / `cambiar-esta-clave`
(igual que el frontend). Override con `ADMIN_USER` / `ADMIN_PASSWORD`.

## Correr

```bash
./mvnw spring-boot:run
# o con el perfil local:
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Al primer arranque, Hibernate crea el esquema (`ddl-auto=update`) y el
`DataSeeder` carga parametrías, escalas de talle, descuentos y 10 productos de
ejemplo. Para no sembrar: `SEED_ENABLED=false`.

### Crear el esquema con scripts SQL (deploy)

En `database/` hay scripts para armar la base a mano — para producción o para no
depender de `ddl-auto`:

```bash
mysql -u root -p < database/setup.sql   # todo junto: base + tablas + config
```

Después corré la app con `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (chequea que el
esquema coincida) o `none`. Ver `database/README.md`.

## Probar

```bash
# catálogo público
curl http://localhost:8080/api/products

# login → token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"cambiar-esta-clave"}' | jq -r .token)

# endpoint admin con token
curl http://localhost:8080/api/admin/products -H "Authorization: Bearer $TOKEN"

# sin token → 401
curl -i -X POST http://localhost:8080/api/admin/products
```

## Tests

```bash
./mvnw test
```
Los tests usan **H2 en memoria** (no tocan MySQL).

## Estructura (package-by-layer)

```
com.estilospequenos
  BackendApplication
  model/        entidades JPA (Product, Order, ParamGroup, SizeScale, Supplier, Discount…)
  repository/   interfaces Spring Data (*Repository)
  service/      lógica de negocio (*Service)
  controller/   endpoints REST (*Controller, incl. AuthController)
  dto/          records de request/response (*Dtos, LoginRequest, TokenResponse)
  common/       ApiError, excepciones, utils (Slugs)
  config/       seguridad/JWT, CORS, OpenAPI, DataSeeder, manejo de errores
```

## Endpoints

| Ámbito | Ruta base |
|---|---|
| Público | `GET /api/products`, `GET /api/products/{id}`, `GET /api/param-groups`, `GET /api/size-scales`, `GET /api/hero-slides`, `POST /api/orders` |
| Auth | `POST /api/auth/login` |
| Admin (JWT) | `/api/admin/products`, `/api/admin/param-groups`, `/api/admin/size-scales`, `/api/admin/suppliers`, `/api/admin/discounts`, `/api/admin/orders`, `/api/admin/hero-slides` |

Detalle completo en Swagger UI.

## Pendientes

- Migraciones con Flyway (hoy `ddl-auto=update`).
- Hashear la clave del admin (hoy comparación directa; el `PasswordEncoder` ya está registrado).
- Conectar el frontend Angular (reemplazar los services de `localStorage` por `HttpClient`).
- Deploy / perfil de producción.
