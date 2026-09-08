# Estilos Pequeños — Backend · documento de detalle

> Documento vivo del backend. El overview general del proyecto (front + back)
> está en `../frontend-ecommerce---ruth/PROYECTO.md`. Este archivo entra en el
> detalle de la API.

Última actualización: 2026-09-08.

---

## 1. Qué es / contexto

API REST del ecommerce **Estilos Pequeños** (indumentaria infantil, Argentina).
Sirve dos cosas:

- **Catálogo público**: productos, parametrías, escalas de talle, carrusel y la
  creación de pedidos del checkout (sin login).
- **Panel de administración**: CRUD de todo lo anterior + proveedores +
  descuentos + gestión de pedidos, protegido con **JWT**.

El checkout no tiene pasarela de pago: el frontend arma un mensaje de WhatsApp
con el pedido; el pedido se guarda acá (con código `PED-XXXX`) para que el
dueño/a lo gestione desde el panel.

**Historia:** el proyecto arrancó 100% frontend con datos en `localStorage`.
El 2026-09-08 se hizo este backend (v1) y se conectó el frontend Angular
(sus services ahora hablan con `/api/*`).

---

## 2. Stack y decisiones

| Tema | Elección | Por qué / alternativas |
|---|---|---|
| Lenguaje / runtime | **Java 21** | pedido del cliente |
| Framework | **Spring Boot 3.3.5** | estándar para REST + Security + JPA |
| Build | **Maven** con wrapper (`./mvnw`) | no depende de Maven global; sin Gradle |
| Base de datos | **MySQL 8** | pedido del cliente. Local: `root`/`root`, DB `estilos_pequenos` (se crea sola) |
| ORM / esquema | Spring Data JPA + Hibernate, `ddl-auto=update` | Flyway queda pendiente. Hay scripts SQL en `database/` para el modo controlado |
| Auth | **JWT stateless** (HS256, `io.jsonwebtoken` 0.12.6) | un solo admin, sin sesión de servidor |
| Contraseñas | **BCrypt** (tabla `admin_user`) | nunca texto plano |
| Docs de API | **springdoc-openapi** → Swagger UI en `/swagger-ui.html` | |
| Boilerplate | Lombok | |
| Organización | **package-by-layer** (`model/`, `repository/`, `service/`, `controller/`, `dto/`) | pedido del cliente (estilo MVC clásico) |

Paquete base: `com.estilospequenos`. Puerto: `8080`.

---

## 3. Cómo correr

### Requisitos
- JDK 21 (`java -version` → 21.x).
- MySQL 8 corriendo en `localhost:3306`. La base `estilos_pequenos` se crea sola
  (`createDatabaseIfNotExist=true`). En esta PC: servicio `MySQL80`, usuario/clave
  `root` / `root` (ya es el default del `application.yml`).

### Arrancar
```bash
cd backend
./mvnw spring-boot:run
```
Al primer arranque Hibernate crea las tablas y el `DataSeeder` carga datos de
ejemplo (ver sección 8). Queda en `http://localhost:8080`.

### Variables de entorno (opcionales, para override)
| Var | Default | Qué es |
|---|---|---|
| `DB_USER` / `DB_PASSWORD` | `root` / `root` | credenciales MySQL |
| `JWT_SECRET` | dev (aviso en el log) | **en prod: definir**, ≥ 32 chars |
| `JWT_EXPIRATION_MINUTES` | `720` (12 h) | vida del token |
| `ADMIN_USER` / `ADMIN_PASSWORD` | `admin` / `ruth123` | admin **inicial** (ver sección 7) |
| `ADMIN_RECOVERY` | `frase-de-recuperacion-cambiar` | frase de recuperación inicial |
| `CORS_ORIGINS` | `http://localhost:4200,http://localhost:4300` | orígenes permitidos |
| `SEED_ENABLED` | `true` | cargar datos de ejemplo |
| `SERVER_PORT` | `8080` | |

Alternativa a las env vars: copiar `src/main/resources/application-local.yml.example`
a `application-local.yml` (gitignored) y correr con
`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.

### Tests
```bash
./mvnw test          # 9 tests, usan H2 en memoria (no tocan MySQL)
```

---

## 4. Estructura del código (package-by-layer)

```
com.estilospequenos
  BackendApplication            excluye UserDetailsServiceAutoConfiguration (auth es por JWT)
  model/        entidades JPA
                AdminUser, Product + SizeStock + ProductParam (embeddables),
                ParamGroup + ParamOption, SizeScale, Supplier,
                Discount + DiscountConfig, Order + OrderLine + OrderStatus(enum),
                HeroSlide
  repository/   interfaces Spring Data (*Repository)
  service/      lógica de negocio (*Service)
                AuthService, ProductService, ParamService, SizeScaleService,
                SupplierService, DiscountService, OrderService, HeroSlideService
  controller/   endpoints REST (*Controller)
                AuthController, AccountController, ProductController, ParamController,
                SizeScaleController, SupplierController, DiscountController,
                OrderController, HeroSlideController
  dto/          records de request/response
                LoginRequest, TokenResponse, AccountDtos, ProductDtos, ParamDtos,
                SizeScaleDtos, SupplierDtos, DiscountDtos, OrderDtos, HeroSlideDtos
  common/       ApiError (cuerpo de error uniforme), ResourceNotFoundException,
                BadRequestException, Slugs (genera ids legibles)
  config/       SecurityConfig, JwtService, JwtAuthFilter, OpenApiConfig,
                AppProperties (@ConfigurationProperties app.*), DataSeeder,
                GlobalExceptionHandler (@RestControllerAdvice)
```

Los ids son `String`: UUID para lo que se crea por API, slug fijo para los
seeds (`grp-publico`, `escala-bebe`, `publico-bebe`…) — así coinciden con lo
que el frontend espera y la migración es determinista.

`OSIV` (`spring.jpa.open-in-view`) está en `true`: permite mapear entidades a
DTO en el controller sin `LazyInitializationException`. A futuro: proyecciones
DTO en el service y desactivarlo.

---

## 5. Modelo de datos

| Entidad | Campos clave | Notas |
|---|---|---|
| **AdminUser** | `id, username (unique), passwordHash, recoveryHash?, enabled, createdAt` | Contraseña y frase de recuperación **hasheadas con BCrypt**. Un solo registro en la práctica. |
| **SiteSettings** | fila única `id='config'`, `storeName, whatsappNumber, aboutText?, instagram?, facebookUrl?` | Datos del local editables desde el panel. `whatsappNumber` valida `\d{8,15}`. `instagram` se guarda sin `@`. |
| **Product** | `id, name, description, price, ageRange, imageUrl (MEDIUMTEXT), active, createdAt, sizeScaleId?, supplierId?, costPrice?` | `imageUrl` admite data URI. `supplierId`/`costPrice` = info interna, **no** se exponen en el catálogo público. |
| — `sizeStocks` | `List<SizeStock{size, stock}>` (`@ElementCollection` → `product_size_stock`) | stock por talle |
| — `params` | `Set<ProductParam{groupId, optionId}>` (`@ElementCollection` → `product_param`) | en el DTO se expone como `Map<String,List<String>>` |
| **ParamGroup** | `id, name, multiple, showInCatalog, system` + `@OneToMany options` | grupos de clasificación (Público / Tipo / Estación). `system` = no se puede borrar. |
| **ParamOption** | `id, label, position` | |
| **SizeScale** | `id, name, system` + `values: List<String>` ordenada | escalas de talle (ropa bebé/niños/adultos, calzado) |
| **Supplier** | `id, name, phone?, address?, notes?` | proveedores del local |
| **Discount** | `id, kind (MONTO\|PARAMETRO), discountPercent, enabled, label?, minAmount?, groupId?, optionId?` | |
| **DiscountConfig** | fila única `id='config'`, `combineMode (MEJOR\|COMBINAR)` | |
| **Order** | `id, number (unique), customerName, subtotal, discountPercent, discountAmount, total, status (PENDIENTE\|PROCESADO\|CANCELADO), createdAt, processedAt?` | `code` = `"PED-" + %04d(number)` (getter `@Transient`). `number` se deriva de `MAX(number)+1`. |
| — `lines` | `List<OrderLine{id, productId, productName, size, quantity, unitPrice, accepted}>` | `productName` se guarda por si el producto cambia después |
| **HeroSlide** | `id, imageUrl (MEDIUMTEXT), alt, position` | fotos del carrusel de la home |

Enums en MAYÚSCULA (así los devuelve la API y así los espera el frontend).

---

## 6. Endpoints

Base: `/api`. Errores → cuerpo `ApiError` (`{timestamp, status, error, message, fieldErrors}`).

### Públicos (sin token)

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/auth/login` | `{username, password}` → `{token, tokenType, expiresAt}` |
| `POST` | `/api/auth/recover` | `{username, recoveryPhrase, newPassword}` → setea la pass nueva y devuelve un token |
| `GET` | `/api/products` | productos `active=true` (catálogo) |
| `GET` | `/api/products/{id}` | ficha |
| `GET` | `/api/param-groups` | parametrías (filtros del catálogo) |
| `GET` | `/api/size-scales` | escalas de talle |
| `GET` | `/api/hero-slides` | carrusel |
| `GET` | `/api/discounts` | `{discounts, combineMode}` — para el preview del descuento en el carrito |
| `GET` | `/api/settings` | datos del local: `{storeName, whatsappNumber, aboutText, instagram, facebookUrl}` — los usan header, footer, home y el armado del mensaje de WhatsApp |
| `POST` | `/api/orders` | crea el pedido desde el carrito: `{customerName, items:[{productId, size, quantity}]}` → calcula `code`, descuentos y totales |

### Admin (`/api/admin/**` — requieren `Authorization: Bearer <jwt>`)

| Recurso | Endpoints |
|---|---|
| **account** | `GET /api/admin/account` → `{username, hasRecoveryPhrase}` · `PUT /account/password` `{currentPassword, newPassword}` · `PUT /account/recovery` `{currentPassword, recoveryPhrase}` |
| **settings** | `GET /api/admin/settings` · `PUT /api/admin/settings` `{storeName, whatsappNumber, aboutText?, instagram?, facebookUrl?}` (misma respuesta que el GET público) |
| **products** | `GET` (todos, incl. inactivos) · `POST` · `GET/PUT/DELETE /{id}` · `PATCH /{id}/active` `{active}` · `PATCH /{id}/stock` `{size, stock}` |
| **param-groups** | `GET` · `POST` · `PUT/DELETE /{id}` (DELETE bloqueado si `system`) · `POST /{id}/options` · `PUT/DELETE /{id}/options/{optionId}` |
| **size-scales** | `GET` · `POST` · `PUT/DELETE /{id}` (DELETE bloqueado si `system`) · `PUT /{id}/values` `{values}` (reemplaza la lista) |
| **suppliers** | `GET` · `POST` · `GET/PUT/DELETE /{id}` |
| **discounts** | `GET` · `POST` · `PUT/DELETE /{id}` · `GET/PUT /api/admin/discounts/config` `{combineMode}` |
| **orders** | `GET` · `GET /pending-count` → `{pending}` · `GET /{id}` · `PUT /{id}/lines` `{lines:[{lineId, accepted}]}` · `POST /{id}/confirm` (descuenta stock de las líneas `accepted`, estado→PROCESADO) · `POST /{id}/cancel` |
| **hero-slides** | `GET` · `POST` · `PUT/DELETE /{id}` · `PUT /reorder` `{ids:[...]}` |

### Cálculo de descuentos (server-side)

`DiscountService.computeForLines(...)` — port de `discount.service.ts`
(`computeCartDiscount`) del frontend:
- **por parámetro**: por ítem, el `%` más alto de un descuento habilitado cuyo
  `(groupId, optionId)` esté en `product.params`;
- **por monto**: el mejor tier habilitado alcanzado;
- modo `MEJOR` (se usa el que más ahorra, no acumula) / `COMBINAR` (parámetro por
  ítem + monto sobre el subtotal ya rebajado);
- `discountPercent` efectivo = `round(amount / subtotal * 100)`.

Lo usan `POST /api/orders` (al crear) — `confirm` no recalcula, solo descuenta
stock.

---

## 7. Autenticación

- **Login** (`POST /api/auth/login`): valida `username` + `password` contra la
  tabla `admin_user` con `passwordEncoder.matches` (BCrypt). Devuelve un JWT
  (HS256, claim `sub` = username, `role` = ADMIN, exp según config).
- **`JwtAuthFilter`**: lee `Authorization: Bearer <jwt>`, valida firma + exp, y
  si es válido setea un `Authentication` con authority `ROLE_ADMIN`.
- **`SecurityConfig`**: stateless, CSRF off, CORS por `app.cors.allowed-origins`.
  `/api/admin/**` → `hasRole('ADMIN')`; `/api/auth/**`, los GET del catálogo,
  `GET /api/discounts`, `GET /api/settings`, `POST /api/orders` y Swagger → `permitAll`.
  401 y 403 responden con `ApiError` en JSON.

### Admin inicial y recuperación

- Al arrancar, `DataSeeder` → `AuthService.ensureInitialAdmin()`: si no hay un
  `admin_user` con el username de `app.admin.username`, lo crea con
  `app.admin.password` y `app.admin.recovery-phrase` **hasheados**. Si ya existe
  pero le falta la frase de recuperación, se la completa.
- Defaults: usuario **`admin`**, contraseña **`ruth123`**, frase de recuperación
  **`frase-de-recuperacion-cambiar`**. **Cambiar los tres** (desde `/admin/cuenta`
  en el front, o por env vars antes del primer arranque).
- **Recuperar** (olvido de contraseña, `POST /api/auth/recover`): usuario + frase
  de recuperación + contraseña nueva → si la frase coincide, cambia la pass y
  devuelve un token (queda logueado). No usa email.
- **Cambiar** contraseña / frase: `PUT /api/admin/account/password` y
  `/recovery` (piden la contraseña actual). El username sale del JWT.
- Mínimo de contraseña / frase: 4 caracteres.
- **Reset de emergencia** (si se olvidan las dos cosas): actualizar
  `password_hash` / `recovery_hash` de la fila en la base con un hash BCrypt
  nuevo (generable con `BCryptPasswordEncoder().encode("...")`), o correr de
  nuevo `database/seed.sql` (deja `admin` / `ruth123`).

---

## 8. DataSeeder

`config/DataSeeder.java` (`CommandLineRunner`). El admin inicial y la fila de
`site_settings` se crean **siempre** (get-or-create con los valores por defecto);
el resto solo si `SEED_ENABLED=true` **y** la tabla está vacía:

- **ParamGroups**: `grp-publico` (system) Bebé/Nena/Nene/Unisex, `grp-tipo`
  9 opciones, `grp-estacion` (multiple) 5 opciones — mismos ids que el front.
- **SizeScales**: 5 escalas seed (`escala-bebe`, `escala-ninos`, `escala-adultos`,
  `escala-calzado-ninos` 17-34, `escala-calzado-adultos` 34-46).
- **Discounts**: 2 por monto ($100.000→20%, $200.000→25%) + `DiscountConfig(MEJOR)`.
- **Products**: 10 de ejemplo, con `params`, `sizeScaleId`, `sizeStocks` y una
  imagen SVG data-URI mínima (emoji sobre círculo de color).
- Suppliers / HeroSlides: vacío.

Cada bloque chequea su propia tabla, así correr `seed.sql` primero y después la
app no genera duplicados.

---

## 9. Base de datos — `database/`

| Archivo | Qué hace |
|---|---|
| `setup.sql` | **todo junto**: crea la base + las 15 tablas + la config base (admin, `site_settings`, parametrías, escalas, descuentos). Es el de deploy. |
| `schema.sql` | solo las tablas |
| `seed.sql` | solo la config base (idempotente, `INSERT ... ON DUPLICATE KEY UPDATE`) |
| `reset.sql` | drop + create de la base vacía |

`setup.sql` = `schema.sql` + `seed.sql` concatenados (hay una nota de cómo
regenerarlo).

- **Desarrollo local**: no hace falta correr nada, la app usa `ddl-auto=update`
  + `createDatabaseIfNotExist=true`.
- **Deploy**: `mysql < database/setup.sql` una vez, y correr la app con
  `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (Hibernate solo verifica) o `none`.
- `schema.sql` se mantiene a mano en sync con las entidades; se puede regenerar
  la referencia cruda con la property `jakarta.persistence.schema-generation`
  (ver `database/README.md`).

---

## 10. Manejo de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) traduce todo a `ApiError`:

| Excepción | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BadRequestException` (regla de negocio, ej: borrar parametría de sistema) | 400 |
| `MethodArgumentNotValidException` (validación de DTO) | 400 + `fieldErrors` |
| `BadCredentialsException` / otras `AuthenticationException` | 401 |
| `AccessDeniedException` | 403 |
| resto | 500 |

---

## 11. Pendientes

- **Flyway** para migraciones versionadas (hoy `ddl-auto=update` + scripts a mano).
- Proyecciones DTO en el service y desactivar OSIV.
- Perfil `prod` (`application-prod.yml`) + pipeline de deploy.
- Rate-limiting / lockout en el login.
- Multi-admin (hoy hay uno solo; el modelo `AdminUser` ya lo soporta).
- Pantalla / endpoints de métricas (ventas por talle / proveedor / parametría),
  leyendo `Order` + `OrderLine` (la data ya está estructurada para eso).
- Subida de imágenes a storage en vez de data-URI en la base.

---

## 12. Historial

1. **2026-09-08 — v1 del backend**: Spring Boot 3.3 + Java 21 + MySQL + JWT,
   package-by-feature. CRUD del admin + catálogo público + `POST /api/orders`
   con cálculo de descuentos server-side. `DataSeeder` con 10 productos.
2. Scripts SQL en `database/` (schema / seed / reset / setup).
3. **Reorganización a package-by-layer** (`model/`, `repository/`, `service/`,
   `controller/`, `dto/`).
4. **Admin en la DB con BCrypt**: entidad `AdminUser` + `AuthService`; el login
   valida contra la tabla. Default `admin` / `ruth123`.
5. **Conexión con el frontend**: se sumó `GET /api/discounts` (público) para el
   preview del carrito.
6. **Recuperación de contraseña + gestión de cuenta**: campo `recoveryHash` en
   `AdminUser`, `POST /api/auth/recover` (frase de recuperación, sin email),
   `PUT /api/admin/account/password` y `/recovery`. Frase inicial
   `frase-de-recuperacion-cambiar`.
7. **Datos del local configurables** (2026-09-08): entidad `SiteSettings` (fila
   única `id='config'`), `GET /api/settings` (público) +
   `GET`/`PUT /api/admin/settings`. La crea `DataSeeder` con los valores actuales
   por defecto. Permite cambiar nombre de tienda, WhatsApp y redes sin
   redesplegar. Tabla `site_settings` sumada a los scripts SQL.
