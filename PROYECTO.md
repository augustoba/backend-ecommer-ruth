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
                Discount, Order + OrderLine + OrderStatus/DeliveryMethod/PaymentMethod (enums),
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
| **SiteSettings** | fila única `id='config'`, `storeName, whatsappNumber, aboutText?, instagram?, facebookUrl?, logoUrl?, whatsappIntro?, whatsappClosing?, storeAddress?, payment{Transfer,QrTransfer,QrCard}Enabled, paymentTransferAlias?, paymentQr{Transfer,Card}Image?, paymentCardLink?, paymentCashEnabled` | Datos del local editables desde el panel. `whatsappNumber` valida `\d{8,15}`. `instagram` sin `@`. `logoUrl`/`paymentQr*Image` = data URI (MEDIUMTEXT). `whatsappIntro`/`whatsappClosing` = saludo/cierre (tokens `{tienda}`/`{codigo}`; el mensaje se arma en el front). `storeAddress` = dirección para el retiro. Cada medio de pago tiene su `*Enabled` (bool) aparte del dato → se ofrece si está habilitado **y** tiene el dato (efectivo sólo el bool). |
| **Product** | `id, name, description, price, ageRange, active, discontinued, createdAt, sizeScaleId?, supplierId?, costPrice?, lowStockThreshold?` | `supplierId`/`costPrice` = info interna, **no** se exponen en el catálogo público. `lowStockThreshold` (unidades por talle) = umbral propio para la alerta de reposición; null = default global (`DashboardService.DEFAULT_LOW_STOCK` = 3). `discontinued` = "no reponer": sigue publicado/vendible pero `DashboardService.lowStock()` lo excluye. |
| — `images` | `List<String>` ordenada (`@ElementCollection` → `product_image`) | fotos, URL o data URI. `idx 0` = portada. En el DTO va como `images[]` + `imageUrl` (la portada, getter `@Transient`, por compat con tarjetas/carrito). El request pide `images` (`@NotEmpty`). |
| — `sizeStocks` | `List<SizeStock{size, stock}>` (`@ElementCollection` → `product_size_stock`) | stock por talle |
| — `params` | `Set<ProductParam{groupId, optionId}>` (`@ElementCollection` → `product_param`) | en el DTO se expone como `Map<String,List<String>>` |
| **ParamGroup** | `id, name, multiple, showInCatalog, system` + `@OneToMany options` | grupos de clasificación (Público / Tipo / Estación). `system` = no se puede borrar. |
| **ParamOption** | `id, label, position` | |
| **SizeScale** | `id, name, system` + `values: List<String>` ordenada | escalas de talle (ropa bebé/niños/adultos, calzado) |
| **Supplier** | `id, name, phone?, address?, notes?` | proveedores del local |
| **Discount** | `id, kind (MONTO\|PARAMETRO\|PAGO\|ENVIO_GRATIS), discountPercent, enabled, stackable, label?, detail?, startsAt?, endsAt?, minAmount?, groupId?, optionId?, paymentMethods?` | `stackable` = acumulable. `detail` = letra chica configurable. `paymentMethods` = CSV de `PaymentMethod` (kind PAGO). `minAmount` sirve para MONTO y ENVIO_GRATIS. `activeNow()` = enabled + rango. DTO expone `status`. **Ya no existe `DiscountConfig`/combineMode**. |
| **Order** | `id, number (unique), customerName, subtotal, discountPercent, discountAmount, total, status (PENDIENTE\|PROCESADO\|CANCELADO), deliveryMethod (PICKUP\|SHIPPING), shippingAddress?, shippingReference?, shippingLat?, shippingLng?, paymentMethod? (TRANSFER\|QR_TRANSFER\|QR_CARD\|CASH), createdAt, processedAt?` | `code` = `"PED-" + %04d(number)` (getter `@Transient`). `number` = `MAX(number)+1`. `deliveryMethod` default PICKUP (pedidos viejos). El envío **no** se cotiza: no hay costo en el `Order`, se coordina aparte. `OrderService.create` exige `shippingAddress` si `deliveryMethod=SHIPPING`. |
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
| `GET` | `/api/discounts` | `{discounts}` — para el preview del descuento en el carrito |
| `GET` | `/api/settings` | datos del local (`SettingsResponse` completo: nombre, WhatsApp, redes, logo, textos del mensaje, dirección, medios de pago) — los usan header, footer, home, checkout y el armado del mensaje |
| `POST` | `/api/orders` | crea el pedido desde el carrito: `{customerName, items:[{productId, size, quantity}], deliveryMethod?, shippingAddress?, shippingReference?, shippingLat?, shippingLng?, paymentMethod?}` → calcula `code`, descuentos y totales; exige dirección si `deliveryMethod=SHIPPING` |

### Admin (`/api/admin/**` — requieren `Authorization: Bearer <jwt>`)

| Recurso | Endpoints |
|---|---|
| **account** | `GET /api/admin/account` → `{username, hasRecoveryPhrase}` · `PUT /account/password` `{currentPassword, newPassword}` · `PUT /account/recovery` `{currentPassword, recoveryPhrase}` |
| **settings** | `GET /api/admin/settings` · `PUT /api/admin/settings` `{…, storeAddress?, payment{Transfer,QrTransfer,QrCard}Enabled?, paymentTransferAlias?, paymentQr{Transfer,Card}Image?, paymentCardLink?, paymentCashEnabled?}` (misma respuesta que el GET público) |
| **metrics** | `GET /api/admin/metrics?from=YYYY-MM-DD&to=YYYY-MM-DD&groupBy=grp-tipo` → totales, serie mensual, top/bottom productos y desglose por grupo de parametría. `GET /api/admin/metrics/comparison?year=` → comparativas mes a mes y semana a semana. Ver §6bis. |
| **dashboard** | `GET /api/admin/dashboard` → resumen del panel (pedidos pendientes, facturación del mes, conteo de productos, últimos 6 pedidos, `defaultLowStockThreshold`, lista de talles por reponer). `GET /api/admin/low-stock` → sólo la lista de talles por reponer (para el badge del menú). `DashboardService`. |
| **products** | `GET?page&size` (**paginado**, `{content, page, size, totalElements, totalPages}`; todos, incl. inactivos) · `POST` · `GET/PUT/DELETE /{id}` · `PATCH /{id}/active` `{active}` · `PATCH /{id}/discontinued` `{discontinued}` (marcar "no reponer") · `PATCH /{id}/stock` `{size, stock}` |
| **param-groups** | `GET` · `POST` · `PUT/DELETE /{id}` (DELETE bloqueado si `system`) · `POST /{id}/options` · `PUT/DELETE /{id}/options/{optionId}` |
| **size-scales** | `GET` · `POST` · `PUT/DELETE /{id}` (DELETE bloqueado si `system`) · `PUT /{id}/values` `{values}` (reemplaza la lista) |
| **suppliers** | `GET` · `POST` · `GET/PUT/DELETE /{id}` |
| **discounts** | `GET` · `POST` · `PUT/DELETE /{id}` (request: `kind, discountPercent, enabled?, stackable?, label?, detail?, startsAt?, endsAt?, minAmount?, groupId?, optionId?, paymentMethods?`) |
| **orders** | `GET?page&size&search&status&from&to` (**paginado** + filtros: `search` = nombre del cliente o número/código de pedido, `status`, `from`/`to` sobre la fecha de creación) · `GET /pending-count` → `{pending}` · `GET /{id}` · `PUT /{id}/lines` `{lines:[{lineId, accepted}]}` · `POST /{id}/confirm` (**estricto**: 400 con detalle si falta stock en alguna línea aceptada — no toca nada; si alcanza, descuenta y estado→PROCESADO) · `POST /{id}/cancel` — las 3 mutaciones devuelven el pedido actualizado |
| **hero-slides** | `GET` · `POST` · `PUT/DELETE /{id}` · `PUT /reorder` `{ids:[...]}` |

### Cálculo de descuentos (server-side)

`DiscountService.computeForLines(items, paymentMethod, deliveryMethod)` — port de
`discount.service.ts` (`computeCartDiscount`) del frontend:
- sólo entran los descuentos **vigentes** (`activeNow()`);
- se arma una "instancia" por descuento que aplica: **parámetro** (por ítem, el `%`
  más alto), **monto** (mejor tier alcanzado), **pago** (kind PAGO cuyo
  `paymentMethods` contiene el elegido), con el monto que ahorraría **si fuera solo**;
- **combinación**: si TODAS las instancias son `stackable` → cascada (parámetro,
  monto, pago, cada una sobre lo que va quedando); si hay una NO stackable → sólo
  la instancia que más ahorra;
- **ENVIO_GRATIS**: aparte, informativo (`freeShipping` en el resultado) cuando
  `deliveryMethod=SHIPPING` y el subtotal supera `minAmount`. `OrderService.create`
  lo guarda en `Order.freeShippingNote`; los `detail` aplicados van a `Order.discountNote`;
- `discountPercent` efectivo = `round(amount / subtotal * 100)`.

Lo usan `POST /api/orders` (al crear) — `confirm` no recalcula, solo descuenta
stock.

### 6bis. Métricas de ventas (`MetricsService`)

`GET /api/admin/metrics` — se calcula sobre las **líneas aceptadas** de los
pedidos **`PROCESADO`**, ubicando cada venta por `processedAt` (la fecha en que
se confirmó). `revenue` = `unitPrice × quantity` (precio de lista; **no** aplica
el descuento del pedido, que es a nivel total).

- **Parámetros** (todos opcionales): `from`, `to` (ISO date, inclusivos; por
  defecto primer día de hace 11 meses → hoy), `groupBy` (id de `ParamGroup`
  para el desglose; por defecto `grp-tipo`).
- **Respuesta** (`MetricsDtos.MetricsResponse`): `totals {revenue, units, orders}`,
  `byMonth[]` (serie **continua**, meses sin ventas en 0), `topProducts[]` (más
  vendidos por unidades, máx. 10), `bottomProducts[]` (menos vendidos —
  **incluye productos activos con 0 ventas**, máx. 10), `byGroup {groupId,
  groupName, rows[]}` (unidades y facturación por opción del grupo).
- Rango inválido (`from` > `to`) → 400.

**`GET /api/admin/metrics/comparison?year=2026`** — comparativas del año
(`MetricsService.compareYear`): `monthly[]` = facturación total mes a mes (hasta
el mes actual), `weekly[]` = ídem pero sólo el mismo tramo de días que la
"semana en curso" del mes actual (bloques de 7 días — 1–7, 8–14, 15–21, 22–28,
29–fin — cortados en el día de hoy, para comparar "lo que va de la semana"
contra el mismo punto de los meses anteriores). `week {weekOfMonth, dayFrom,
dayTo}` describe esa ventana. `year` por defecto: el actual.

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
- **Discounts**: 2 por monto ($100.000→20%, $200.000→25%), no acumulables.
- **Products**: 10 de ejemplo, con `params`, `sizeScaleId`, `sizeStocks` y una
  imagen SVG data-URI mínima (emoji sobre círculo de color).
- Suppliers / HeroSlides: vacío.

Cada bloque chequea su propia tabla, así correr `seed.sql` primero y después la
app no genera duplicados.

---

## 9. Base de datos — `database/`

| Archivo | Qué hace |
|---|---|
| `setup.sql` | **todo junto**: crea la base + las 16 tablas + la config base (admin, `site_settings`, parametrías, escalas, descuentos). Es el de deploy. |
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
- Métricas: falta el desglose por **talle** y por **proveedor** (hoy están
  totales, serie mensual, top/bottom productos y desglose por parametría —
  §6bis). También: aplicar el descuento del pedido a la facturación.
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
8. **Métricas de ventas** (2026-09-08, §6bis): `GET /api/admin/metrics`
   (`MetricsService`) — totales, serie mensual continua, top/bottom productos y
   desglose por grupo de parametría, sobre los pedidos PROCESADO en un rango de
   fechas. Sin cambios de esquema (sólo lectura de `Order`/`OrderLine`).
9. **Comparativas** (2026-09-08): `GET /api/admin/metrics/comparison` —
   facturación mes a mes del año y del mismo tramo de días (semana en curso) mes
   a mes.
10. **Galería de fotos por producto** (2026-09-08): `Product.imageUrl` (campo
    único) → `Product.images` (`List<String>`, tabla `product_image`, `idx 0` =
    portada). El request pide `images` (`@NotEmpty`); la respuesta mantiene
    `imageUrl` (portada, `@Transient`) para no romper tarjetas/carrito.
    **Sin migración**: el backend no está desplegado — recrear la base de dev.
11. **Dashboard + stock bajo** (2026-09-08): `Product.lowStockThreshold`
    (nullable, columna nueva) + `GET /api/admin/dashboard` / `/low-stock`
    (`DashboardService`). Resumen del panel + lista de talles por reponer (stock
    ≤ umbral propio o el default 3). `MetricsService.rangeTotals()` para la
    facturación del mes.
12. **Paginación de los listados del panel** (2026-09-08): `GET /api/admin/products`
    y `/api/admin/orders` pasaron a devolver `PageResponse<T>`
    (`{content, page, size, totalElements, totalPages}`), 20 por página por
    defecto (`?page`/`?size`, tope 100). Los endpoints públicos siguen sin
    paginar.
13. **Confirmación de pedido estricta** (2026-09-08): `OrderService.confirm`
    pre-chequea el stock de todas las líneas aceptadas antes de descontar; si
    algo no alcanza → 400 con el detalle y el pedido queda intacto. (Carrera
    entre dos confirmaciones simultáneas del mismo talle: no cubierta —
    requeriría lock/UPDATE condicional; poco probable con un solo admin.)
14. **Vigencia por fechas en descuentos** (2026-09-08): `Discount.startsAt` /
    `endsAt` (columnas nuevas). El cálculo (`computeForLines`, tiers) sólo usa
    los vigentes. El DTO agrega `status`. Validación `startsAt <= endsAt`.
15. **Filtros en el listado de pedidos** (2026-09-08): `GET /api/admin/orders`
    acepta `search` (nombre del cliente o número/código), `status`, `from`/`to`
    (fecha de creación). `OrderRepository.search(...)` con `@Query` de params
    nulables + `OrderService.search()`.
16. **Flag "no reponer" en productos** (2026-09-08): `Product.discontinued`
    (`BIT NOT NULL DEFAULT 0`, columna nueva) + `PATCH /api/admin/products/{id}/discontinued`
    (`ProductService.setDiscontinued`). `DashboardService.lowStock()` saltea los
    discontinuados, así un producto que no se va a reponer deja de figurar en las
    alertas de stock bajo sin dejar de venderse. **Sin migración**: recrear la
    base de dev (o `schema.sql`/`setup.sql`, ya actualizados).
17. **Logo y textos del mensaje de WhatsApp en `SiteSettings`** (2026-09-08):
    columnas nuevas `logo_url` (`MEDIUMTEXT`, data URI), `whatsapp_intro` y
    `whatsapp_closing` (`VARCHAR(2000)`). `SettingsRequest`/`Response` + service
    las manejan (`blankToNull`; `defaults()` siembra los textos por defecto). El
    mensaje de pedido se sigue armando en el frontend — el backend sólo guarda.
    **Sin migración**: `ddl-auto=update` agrega las columnas; `schema.sql`/
    `setup.sql` actualizados.
18. **Entrega y medio de pago en el pedido** (2026-09-08): enums nuevos
    `DeliveryMethod` (PICKUP/SHIPPING) y `PaymentMethod` (TRANSFER/QR_TRANSFER/
    QR_CARD/CASH). `Order` sumó `deliveryMethod` (default PICKUP), `shipping_address`,
    `shipping_reference`, `shipping_lat/lng`, `payment_method`. `OrderService.create`
    valida que un envío traiga dirección (400 si no). `SiteSettings` sumó
    `store_address` + 5 campos de medios de pago (`payment_transfer_alias`,
    `payment_qr_transfer_image`, `payment_qr_card_image`, `payment_card_link`,
    `payment_cash_enabled`). El envío **no se cotiza**: sin costo en el `Order`.
    **Sin migración** (`ddl-auto=update`); `schema.sql`/`setup.sql` al día.
19. **Motor de descuentos v2** (2026-09-08): `Discount.Kind` sumó **PAGO** y
    **ENVIO_GRATIS**; cada descuento tiene `stackable` (acumulable) y `detail`
    (letra chica) + `paymentMethods` (CSV, kind PAGO). **Se eliminó `DiscountConfig`**
    y los endpoints `/api/admin/discounts/config`. `DiscountService.computeForLines`
    reescrito: instancias por descuento, "todos acumulables → cascada / hay uno no
    acumulable → el mejor solo". `Order` sumó `free_shipping_note` y `discount_note`
    (van al mensaje de WhatsApp y al detalle del pedido). **`ddl-auto=update`**:
    en el dev DB Hibernate mapea los enum a `VARCHAR`, así que los valores nuevos
    entran sin ALTER; agrega las columnas nuevas; la tabla `discount_config` queda
    huérfana (inocua). `schema.sql`/`setup.sql` actualizados.
20. **Tanda 2026-09-09** (todo commiteado sin pushear; 28 tests en verde):
    - **Rate limiting** del login/recuperación: `LoginAttemptService` (memoria,
      por IP + usuario), `TooManyRequestsException` → 429 + `Retry-After`.
      Config `app.login-throttle.*` (5 intentos / 15 min → bloqueo 15 min).
    - **Productos:** `POST /api/admin/products/{id}/duplicate`;
      `ProductRepository.search` (nombre, proveedor, parametría, estado,
      `noStock`); **soft-delete** `Product.deleted` (`DELETE` archiva, `get()`
      sigue encontrándolo; `/archived` + `/{id}/restore`).
    - **Pedidos:** `@Max(999)` en `CartItem.quantity`. `OrderService.create` NO
      valida stock (el flujo "el dueño revisa" lo necesita; sólo `confirm` es
      estricto). `GET /api/orders/lookup?code&name` (público, para "mis pedidos").
      `Order.channel` (WEB/LOCAL). `POST /api/admin/orders/pos` (permiso POS_USE):
      crea + confirma en la misma transacción (venta en el local).
    - **Cupones:** entidad `Coupon` + `CouponService` (% o monto, mínimo, tope de
      usos, vencimiento, `stackable`). `GET /api/coupons/{code}` (público, valida
      sin consumir), CRUD admin (`COUPONS_MANAGE`), genera N códigos de una.
      `Order` sumó `coupon_code`/`coupon_discount`; se aplica al crear el pedido.
    - **RBAC:** `Permission` (enum, 14), `Role` (nombre + permisos; system
      "Administrador" = todos), `AdminUser.role`. `JwtAuthFilter` carga los
      permisos por request. `@EnableMethodSecurity` + `@PreAuthorize` en todos
      los controllers de `/api/admin/**`. `GET /api/auth/me`. `RoleController` /
      `AdminUserController` (`USERS_MANAGE`). `DataSeeder` siembra
      "Administrador" + "Vendedor"; el admin inicial y los usuarios sin rol
      quedan como Administrador (backfill).
    - **SiteSettings** sumó `help_text` / `faq_text` (página "cómo comprar" + FAQ).
    - `schema.sql` actualizado (role, role_permission, admin_user.role_id, coupon,
      product.deleted, orders.channel/coupon_*, site_settings.help/faq).
      `ddl-auto=update` agrega todo solo en el dev DB.
