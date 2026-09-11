# Estilos Pequeños — Backend · documento de detalle

> Documento vivo del backend. El overview general del proyecto (front + back)
> está en `../frontend-ecommerce---ruth/PROYECTO.md`. Este archivo entra en el
> detalle de la API.

Última actualización: 2026-09-11.

> **Nota de mantenimiento:** las tablas de las secciones 2-6 se actualizan
> cuando el cambio es grande (como hoy); para el detalle día a día, la
> fuente de verdad es el **Historial** (sección 12, al final) — ahí sí está
> todo, en orden. Si algo de las tablas no cierra con el código, confiá en
> el Historial y en el código antes que en la tabla.

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
| Auth | **JWT stateless** (HS256, `io.jsonwebtoken` 0.12.6) | multi-usuario, login por **DNI** (no username) + roles (RBAC) |
| Contraseñas | **BCrypt** (tabla `admin_user`) | nunca texto plano. Recuperación: mail con contraseña nueva (no hay "frase de recuperación", se sacó) |
| Mail (SMTP) | credenciales en DB (`platform_mail_settings`, singleton) | hoy: Brevo. Editable sólo por superadmin desde `/admin/config/servicios`, sin redeploy |
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
| `ADMIN_NOMBRE`/`ADMIN_APELLIDO`/`ADMIN_DNI`/`ADMIN_EMAIL`/`ADMIN_PASSWORD` | Ruth/Basaury/`11111111`/`ruth@gmail.com`/`ruth123` | cuenta **inicial** de la dueña de la tienda, rol "Administrador" (ver sección 7) |
| `SUPERADMIN_NOMBRE`/`SUPERADMIN_APELLIDO`/`SUPERADMIN_DNI`/`SUPERADMIN_EMAIL`/`SUPERADMIN_PASSWORD` | Augusto/Basaury/`33756194`/`basauryaugusto@gmail.com`/`augusto123` | cuenta **inicial** del superadmin (dueño de la plataforma), rol "Superadmin". **En otro deploy (otro ecommerce), sobreescribir con los datos reales** |
| `BREVO_SMTP_HOST`/`_PORT`/`_USER`/`_KEY`, `MARKETING_FROM_EMAIL` | placeholders (`changeme@...`) | sólo **siembran** `platform_mail_settings` la primera vez; después se edita desde el panel (ver §7 y §12 #23/#24) |
| `CORS_ORIGINS` | `http://localhost:4200,http://localhost:4300` | orígenes permitidos |
| `SEED_ENABLED` | `true` | cargar datos de ejemplo |
| `SERVER_PORT` | `8080` | |

Alternativa a las env vars: copiar `src/main/resources/application-local.yml.example`
a `application-local.yml` (gitignored) y correr con
`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.

### Tests
```bash
./mvnw test          # 28 tests, usan H2 en memoria (no tocan MySQL)
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
| **AdminUser** | `id, dni (unique), nombre, apellido, email (unique), passwordHash, enabled, role, createdAt` | **DNI = identificador de login** (reemplazó a `username`). Sin frase de recuperación (se sacó, ver §7). `role` → `Role`. Hoy 2 filas reales: Ruth (Administrador) y Augusto (Superadmin). |
| **Role** | `id, name (unique), system, permissions: Set<Permission>, createdAt` | `system=true` → **todos** los permisos siempre (incluidos los que se agreguen a futuro), no editable/borrable. Sólo el rol **"Superadmin"** es `system`. "Administrador" y "Vendedor" son roles normales (editables) con una lista fija de permisos. |
| **Permission** | enum, 18 valores | Ver `model/Permission.java`. Los dos más nuevos: `PAYMENTS_MANAGE` (medios de pago, lo tiene "Administrador") y `PLATFORM_SETTINGS_MANAGE` (identidad/logo/WhatsApp/redes/textos/carrusel/servicio de mail — **sólo** lo tiene "Superadmin"). Reemplazaron a `SETTINGS_MANAGE`, que se sacó. |
| **PlatformMailSettings** | fila única `id='config'`, `host, port, username, password, fromAddress` | Credenciales SMTP (hoy Brevo). Editable sólo por superadmin (`PLATFORM_SETTINGS_MANAGE`) desde `/admin/config/servicios`, `GET/PUT /api/admin/platform/mail`. El `GET` nunca devuelve la clave en texto plano (`passwordSet: boolean`). Semilla inicial desde `app.mail.*` / env vars `BREVO_SMTP_*`. |
| **MarketingConfig** | fila única `id='config'`, `enabled, discountPercent, inactivityDays, spendThreshold, dailyEmailCap, couponValidityDays, cooldownDays, emailSubject?, emailBody?, emailImageUrl?` | Config de la campaña automática de cupón por email (ver §12 #23/#24). `email*` admiten los tokens `{tienda}`/`{codigo}`/`{porcentaje}`/`{vencimiento}`; null/vacío = texto por defecto. `emailImageUrl` = data URI (imagen arriba del mail). |
| **MarketingSend** | `id, email, reason (INACTIVE\|VIP), couponCode?, sentAt, status (SENT\|FAILED), errorMessage?, lifetimeSpendSnapshot?, lastOrderAtSnapshot?` | Log de cada envío (o intento) de campaña — historial en `/admin/campanias` + export CSV. Sólo cuenta para el **cooldown** (no repetirle a un mismo cliente) si `status=SENT`; un `FAILED` no bloquea el reintento al otro día. Los que quedan afuera por el **tope diario** ni siquiera generan fila acá, así que al otro día vuelven a entrar en la cuenta (no se pierden). |
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
| **Order** | `id, number (unique), customerName, customerEmail? (2026-09-11, opcional, no bloquea la venta — para la campaña de marketing y la base de clientes, ver §12 #23), subtotal, discountPercent, discountAmount, total, status (PENDIENTE\|PROCESADO\|CANCELADO), deliveryMethod (PICKUP\|SHIPPING), shippingAddress?, shippingReference?, shippingLat?, shippingLng?, paymentMethod? (TRANSFER\|QR_TRANSFER\|QR_CARD\|CASH), createdAt, processedAt?` | `code` = `"PED-" + %04d(number)` (getter `@Transient`). `number` = `MAX(number)+1`. `deliveryMethod` default PICKUP (pedidos viejos). El envío **no** se cotiza: no hay costo en el `Order`, se coordina aparte. `OrderService.create` exige `shippingAddress` si `deliveryMethod=SHIPPING`. |
| — `lines` | `List<OrderLine{id, productId, productName, size, quantity, unitPrice, accepted}>` | `productName` se guarda por si el producto cambia después |
| **HeroSlide** | `id, imageUrl (MEDIUMTEXT), alt, position` | fotos del carrusel de la home |

Enums en MAYÚSCULA (así los devuelve la API y así los espera el frontend).

---

## 6. Endpoints

Base: `/api`. Errores → cuerpo `ApiError` (`{timestamp, status, error, message, fieldErrors}`).

### Públicos (sin token)

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/auth/login` | `{dni, password}` → `{token, tokenType, expiresAt}` |
| `POST` | `/api/auth/forgot-password` | `{dni}` → 204 siempre (no revela si existe). Si existe, genera una contraseña al azar, la manda por mail y la deja cargada — **no** devuelve token, hay que loguearse con la nueva |
| `GET` | `/api/products` | productos `active=true` (catálogo) |
| `GET` | `/api/products/{id}` | ficha |
| `GET` | `/api/param-groups` | parametrías (filtros del catálogo) |
| `GET` | `/api/size-scales` | escalas de talle |
| `GET` | `/api/hero-slides` | carrusel |
| `GET` | `/api/discounts` | `{discounts}` — para el preview del descuento en el carrito |
| `GET` | `/api/settings` | datos del local (`SettingsResponse` completo: nombre, WhatsApp, redes, logo, textos del mensaje, dirección, medios de pago) — los usan header, footer, home, checkout y el armado del mensaje |
| `POST` | `/api/orders` | crea el pedido desde el carrito: `{customerName, customerEmail?, items:[{productId, size, quantity}], deliveryMethod?, shippingAddress?, shippingReference?, shippingLat?, shippingLng?, paymentMethod?, couponCode?}` → calcula `code`, descuentos y totales; exige dirección si `deliveryMethod=SHIPPING`. `customerEmail` es opcional, no bloquea nada (ver §12 #23) |

### Admin (`/api/admin/**` — requieren `Authorization: Bearer <jwt>`)

| Recurso | Endpoints |
|---|---|
| **account** | `GET /api/admin/account` → `{nombre, apellido, dni, email}` · `PUT /account/password` `{currentPassword, newPassword}` (sin permiso especial, es self-service) |
| **settings** | `GET /api/admin/settings` (`hasAnyAuthority('PLATFORM_SETTINGS_MANAGE','PAYMENTS_MANAGE')`) · `PUT /api/admin/settings/platform` (**sólo superadmin**, `PLATFORM_SETTINGS_MANAGE`) `{storeName, whatsappNumber, aboutText?, instagram?, facebookUrl?, logoUrl?, whatsappIntro?, whatsappClosing?, storeAddress?, helpText?, faqText?}` · `PUT /api/admin/settings/payments` (**admin normal**, `PAYMENTS_MANAGE`) `{payment{Transfer,QrTransfer,QrCard}Enabled?, paymentTransferAlias?, paymentQr{Transfer,Card}Image?, paymentCardLink?, paymentCashEnabled?}` — antes era un solo `PUT /api/admin/settings`, se partió en dos (2026-09-11, ver §12 #22) |
| **platform/mail** | `GET/PUT /api/admin/platform/mail` (**sólo superadmin**, `PLATFORM_SETTINGS_MANAGE`) — credenciales SMTP, ver `PlatformMailSettings` en §5 |
| **marketing** | `GET/PUT /api/admin/marketing/config` · `GET /api/admin/marketing/preview` (dry-run) · `POST /api/admin/marketing/run-now` · `GET /api/admin/marketing/history?page&size&reason&status&from&to` — todo bajo `MARKETING_MANAGE` (lo tiene "Administrador"). Ver §12 #23/#24. |
| **users / roles** | `GET/POST/PUT/DELETE /api/admin/users` (ahora con `nombre, apellido, dni, email` en vez de `username`) · `GET/POST/PUT/DELETE /api/admin/roles` · `GET /api/admin/permissions` — todo bajo `USERS_MANAGE`. Asignar un rol `system` (Superadmin) a un usuario **sólo lo puede hacer otro superadmin** (chequeado en el service, no sólo en el frontend). |
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

> **Reescrito 2026-09-11** — el login pasó de `username` a **DNI**, se sumaron
> roles/superadmin, y la recuperación de contraseña pasó de "frase secreta" a
> mail. Ver el porqué en el Historial, §12 #22.

- **Login** (`POST /api/auth/login`): valida `dni` + `password` contra la
  tabla `admin_user` con `passwordEncoder.matches` (BCrypt). Devuelve un JWT
  (HS256, claim `sub` = **dni**, `role` = ADMIN, exp según config).
- **`JwtAuthFilter`**: lee `Authorization: Bearer <jwt>`, valida firma + exp,
  recarga el `AdminUser` **de la base en cada request** (por `dni`) y setea un
  `Authentication` con `ROLE_ADMIN` + un authority por cada `Permission`
  efectivo del rol — así un cambio de rol/permiso tiene efecto inmediato, sin
  esperar a que expire el token.
- **`SecurityConfig`**: stateless, CSRF off, CORS por `app.cors.allowed-origins`,
  `@EnableMethodSecurity` (permisos finos con `@PreAuthorize` por endpoint).
  `/api/admin/**` → `hasRole('ADMIN')` (chequeo grueso); `/api/auth/**`, los GET
  del catálogo, `GET /api/discounts`, `GET /api/settings`, `POST /api/orders` y
  Swagger → `permitAll`. 401 y 403 responden con `ApiError` en JSON.

### Roles: Superadmin vs Administrador vs Vendedor

- **Superadmin** (`Role.system = true`): **todos** los permisos siempre —
  incluidos los que se agreguen a futuro (`Role.effectivePermissions()` hace
  `EnumSet.allOf(Permission.class)` si `system`). No editable, no borrable.
  Pensado para el desarrollador/dueño de la plataforma, no para cada tienda —
  ve además la config de plataforma (identidad, WhatsApp, redes, textos,
  carrusel, servicio de mail: `PLATFORM_SETTINGS_MANAGE`).
- **Administrador**: rol normal (editable desde `/admin/usuarios`, como
  "Vendedor"), sembrado con **todos los permisos operativos de la tienda**
  (pedidos, POS, catálogo, cupones, campañas, métricas, usuarios, **medios de
  pago**) pero **sin** `PLATFORM_SETTINGS_MANAGE` ni `CAROUSEL_MANAGE`. Es el
  rol de la dueña de la tienda.
- **Vendedor**: subconjunto operativo chico (pedidos, POS, cambios, caja,
  métricas), sin cambios en esta tanda.
- **Guardia de seguridad** (`AdminUserService.assertCanAssign`): asignarle a
  alguien un rol `system` (Superadmin) — al crear o editar un usuario — sólo lo
  puede hacer alguien que **ya** es superadmin. Se chequea en el service (no
  sólo se oculta en el combo del frontend), para que un Administrador con
  `USERS_MANAGE` no pueda auto-otorgarse Superadmin pegándole directo a la API.

### Cuentas iniciales

- Al arrancar, `DataSeeder` siembra los roles (`ensureSystemRole()` →
  Superadmin; `ensureRole("Administrador", ...)`; `ensureRole("Vendedor", ...)`)
  y después `AuthService.ensureInitialAdmin()` / `ensureInitialSuperadmin()`:
  si no hay un `admin_user` con ese DNI, lo crea con los datos de
  `app.admin.*` / `app.superadmin.*` (password **hasheada**).
- Defaults locales: Ruth (Administrador) DNI **`11111111`** / **`ruth123`**;
  Augusto (Superadmin) DNI **`33756194`** / **`augusto123`**. **En otro deploy
  (otro ecommerce), sobreescribir `SUPERADMIN_*`/`ADMIN_*` antes del primer
  arranque** — si no, cualquiera que clone este repo arranca con las mismas
  credenciales de superadmin.
- **"Olvidé mi contraseña"** (`POST /api/auth/forgot-password`, sólo DNI):
  **ya no hay frase de recuperación** (se sacó — `AdminUser.recoveryHash` y la
  columna `recovery_hash` no existen más). Si el DNI existe (todo usuario tiene
  email, es obligatorio), le genera una contraseña al azar (10 caracteres),
  se la manda por mail (`AccountMailService`, texto plano, vía
  `PlatformMailSettingsService.buildSender()`) **y recién si el mail se manda
  bien** le pisa la contraseña — así si el envío falla, no se queda sin poder
  entrar con la que ya tenía. La respuesta es **siempre 204**, exista o no el
  DNI (no revela nada). Rate-limit compartido con el login
  (`LoginAttemptService`, por IP + DNI).
- **Cambiar** contraseña ya logueado: `PUT /api/admin/account/password` (pide
  la actual). El DNI sale del JWT. Mínimo 4 caracteres.
- **Reset de emergencia** (si se pierde el acceso a la cuenta Y al mail):
  actualizar `password_hash` de la fila en la base con un hash BCrypt nuevo
  (`BCryptPasswordEncoder().encode("...")`), o correr de nuevo
  `database/seed.sql` (deja a Ruth en `11111111`/`ruth123`; Augusto no está en
  ese script, lo siembra la app al arrancar si no existe).

---

## 8. DataSeeder

`config/DataSeeder.java` (`CommandLineRunner`). Los roles (Superadmin,
Administrador, Vendedor), las dos cuentas iniciales (Ruth y Augusto, ver §7) y
la fila de `site_settings` se crean **siempre** (get-or-create con los valores
por defecto); el resto solo si `SEED_ENABLED=true` **y** la tabla está vacía:

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
regenerarlo). **`setup.sql` tiene drift viejo** (le faltan columnas/tablas de
varias tandas anteriores a esta) — no se tocó en esta tanda, sólo `schema.sql`
y `seed.sql`. Si se necesita provisionar un server nuevo desde cero, revisar
`setup.sql` contra las entidades actuales antes de confiar en él.

**Cambio de esquema grande 2026-09-11** (login por DNI): `admin_user` perdió
`username`/`recovery_hash` y ganó `dni`/`nombre`/`apellido`/`email` (NOT NULL).
Como Hibernate `ddl-auto=update` no puede aplicar eso solo sobre filas
existentes, **hubo que resetear `admin_user`/`role`/`role_permission`** en la
base de dev (drop de esas 3 tablas + reiniciar la app, que las recrea y
resiembra). Si esto se vuelve a desplegar sobre una base con datos viejos,
hace falta el mismo paso.

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

- **Flyway** para migraciones versionadas (hoy `ddl-auto=update` + scripts a mano
  — esto mordió en la tanda del 2026-09-11, ver §9).
- Proyecciones DTO en el service y desactivar OSIV.
- Perfil `prod` (`application-prod.yml`) + pipeline de deploy.
- Subida de imágenes a storage en vez de data-URI en la base.
- **Credenciales de Brevo reales**: hoy están cargadas directo en
  `platform_mail_settings` (vía `/admin/config/servicios` o insertadas a mano),
  no dependen de las env vars `BREVO_SMTP_*` salvo la primera vez. Si se
  resetea esa tabla, hay que volver a cargarlas.
- `setup.sql` desactualizado respecto a las entidades actuales (ver §9) —
  arreglar antes de usarlo para un deploy nuevo.
- ~~Rate-limiting / lockout en el login~~ — hecho (tanda 2026-09-09, §12 #20).
- ~~Multi-admin~~ — hecho (tanda 2026-09-11, §12 #22): roles + Superadmin/Administrador/Vendedor.
- ~~Métricas por talle/proveedor~~ — hecho (tanda 3, §12 #21).

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
21. **Tanda 3 (2026-09-09):**
    - **Cambios de prenda:** `Exchange` + `ExchangeLine` (DEVUELTA/LLEVADA).
      `ExchangeService.create` valida stock de lo que se lleva (estricto),
      suma al stock lo devuelto, descuenta lo que se lleva, `difference` a
      precio de lista. `POST/GET /api/admin/exchanges` (permiso `EXCHANGES_USE`).
      Código `CAM-0001`. `ProductService.incrementStock` / `stockOf` nuevos.
    - **Caja:** `CashRegisterService` + `GET /api/admin/cash-register?date=`
      (permiso `CASH_REGISTER_VIEW`) — total por medio de pago, abierto en
      local / cambios (diferencia) / online (pedidos confirmados ese día).
    - **Permisos nuevos:** `EXCHANGES_USE`, `CASH_REGISTER_VIEW`. El seed
      "Vendedor" los suma. `Permission` pasó de 16 a 18.
    - **Métricas:** `MetricsResponse` sumó `bySize` y `bySupplier`; las
      diferencias positivas de los cambios del período suman a `totalRevenue`
      y al canal LOCAL (no a unidades ni a `orders`). `MetricsService` sumó
      `SupplierRepository` + `ExchangeRepository`.
    - **Best-sellers:** `GET /api/products/best-sellers?limit=` (público) —
      top por unidades de los últimos 90 días, sólo publicados/no archivados.
      `ProductService` sumó `OrderRepository`.
    - **Export CSV:** `CsvExportController` `/api/admin/export/{products,
      orders,exchanges}.csv` + helper `common/Csv`.
    - `schema.sql`: `exchange`, `exchange_line`.
22. **Rol Superadmin + login por DNI + config de plataforma separada
    (2026-09-11).** Pensado para reutilizar este código en otros ecommerce
    (Augusto, el dev, es superadmin en todos; cada cliente tiene su
    "Administrador"). Detalle completo en §7; acá el resumen del cambio:
    - `AdminUser`: `username` → `dni` (nuevo identificador de login) +
      `nombre`/`apellido`/`email` (todos NOT NULL). Se sacó `recoveryHash`
      (ver #24). JWT: subject pasa de username a dni.
    - El rol de sistema (`Role.system=true`, todos los permisos siempre) pasa
      a llamarse **"Superadmin"** (antes "Administrador"). Nuevo
      **"Administrador"**: rol normal (editable) con todo lo operativo de la
      tienda **menos** `PLATFORM_SETTINGS_MANAGE` y `CAROUSEL_MANAGE`.
    - Permisos nuevos `PAYMENTS_MANAGE` y `PLATFORM_SETTINGS_MANAGE`
      reemplazan a `SETTINGS_MANAGE` (se sacó). `/api/admin/settings` se
      partió en `/settings/platform` (superadmin) y `/settings/payments`
      (admin).
    - `AdminUserService.assertCanAssign`: sólo un superadmin puede asignarle
      el rol Superadmin a alguien (chequeo en el service, no sólo cosmético
      en el frontend).
    - Cuentas iniciales sembradas por `DataSeeder`/`AuthService`: Ruth
      (`11111111`/`ruth123`, Administrador) y Augusto
      (`33756194`/`augusto123`, Superadmin) — configurables por
      `ADMIN_*`/`SUPERADMIN_*` (ver §3).
    - **Requirió resetear `admin_user`/`role`/`role_permission`** en la base
      de dev (cambio de esquema incompatible con `ddl-auto=update` sobre
      filas existentes — ver §9). `schema.sql`/`seed.sql` actualizados.
    - Tests de `AuthFlowTest` y los que logueaban por username actualizados a
      DNI.
23. **Campañas de marketing por email (2026-09-11).** Cupón de descuento
    automático a clientes inactivos o de alto gasto, con tope diario para no
    saturar el mail gratuito.
    - `Order.customerEmail` (opcional, no bloquea la venta) capturado en
      checkout web y POS — es la única "base de clientes": no hay entidad
      `Customer` separada, los segmentos se calculan agregando `orders` por
      email (`OrderRepository.findInactiveCustomers`/`findHighSpendCustomers`).
    - `MarketingConfig` (singleton, ver §5): segmentos "inactivos" (sin
      comprar hace N días) y "VIP" (gasto acumulado > umbral); si un email
      califica en los dos, gana VIP. Tope diario + **cooldown** (no repetirle
      al mismo cliente); orden de prioridad: VIP por gasto desc, después
      inactivos por antigüedad.
    - El cupón que se manda es un `Coupon` normal (de un solo uso, generado
      por el `CouponService` que ya existía) — nada nuevo ahí.
    - `MarketingCampaignScheduler` (`@Scheduled`, 06:00) + endpoint de envío
      manual, ambos llaman a `MarketingCampaignService.runNow()`.
      `previewToday()` es de sólo lectura (dry-run) para el botón "ver quién
      calificaría" del panel.
    - **Los que no entran por el tope diario no se pierden**: no se les crea
      fila en `MarketingSend`, así que al otro día vuelven a aparecer como
      candidatos (si siguen calificando) — es un efecto emergente del diseño
      (recompute diario + cooldown sólo para los `SENT`), no una cola
      explícita. Confirmado y con indicador visual agregado en el frontend
      (24-09-11, ver frontend PROYECTO.md).
    - `MarketingSend`: historial + export CSV (`/api/admin/export/marketing.csv`).
    - Permiso `MARKETING_MANAGE` (lo tiene "Administrador", no hace falta ser
      superadmin para correr campañas de la propia tienda).
24. **Servicio de mail configurable + contenido del mail editable + password
    reset por email (2026-09-11, misma tanda que #23 pero en pasadas
    siguientes).**
    - `PlatformMailSettings` (singleton, ver §5): las credenciales SMTP dejan
      de ser sólo variables de entorno (`BREVO_SMTP_*`) y pasan a vivir en la
      base, editables desde `/admin/config/servicios` (sólo superadmin,
      `PLATFORM_SETTINGS_MANAGE`) sin redeploy. Las env vars quedan como
      semilla inicial nada más. `MarketingMailService` y el nuevo
      `AccountMailService` arman el `JavaMailSender` al vuelo con
      `PlatformMailSettingsService.buildSender()` (refactor: antes cada
      servicio armaba el suyo).
    - **Contenido del mail de campaña editable**: `MarketingConfig` sumó
      `emailSubject`/`emailBody`/`emailImageUrl` (tokens `{tienda}`/`{codigo}`/
      `{porcentaje}`/`{vencimiento}`). El mail pasó de texto plano
      (`SimpleMailMessage`) a HTML con imagen embebida inline
      (`MimeMessageHelper`, `cid:banner`) — `MarketingMailService.sendCoupon`
      reescrito.
    - **Password reset por email reemplaza la "frase de recuperación"**: se
      sacó `AdminUser.recoveryHash` (columna `recovery_hash` dropeada),
      `changeRecoveryPhrase`, `RecoverRequest`, `ChangeRecoveryRequest`,
      `PUT /api/admin/account/recovery`. Nuevo `POST /api/auth/forgot-password`
      `{dni}` → 204 siempre (no revela si existe); si existe, genera una
      contraseña de 10 caracteres al azar, la manda por mail **antes** de
      pisar la contraseña guardada (si el mail falla, no se pierde el acceso
      con la vieja) vía `AccountMailService.sendTempPassword`. `AccountResponse`
      perdió `hasRecoveryPhrase`.
    - Probado en vivo contra Brevo real (pedido de prueba + cupón + mail de
      recuperación, después borrados/revertidos).
    - `schema.sql`/`seed.sql` actualizados (sin `recovery_hash`).
