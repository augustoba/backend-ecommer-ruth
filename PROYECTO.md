# Estilos Pequeños — Backend · documento de detalle

> Documento vivo del backend. El overview general del proyecto (front + back)
> está en `../frontend-ecommerce---ruth/PROYECTO.md`. Este archivo entra en el
> detalle de la API.

Última actualización: 2026-09-18 (tanda 2).

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

**Volvimos de una vuelta por el SaaS (2026-09-17):** este mismo código empezó
a evolucionar hacia una plataforma multi-tenant (varios clientes en una sola
base, planes, gating por módulo — `saasweb`). Eso frenaba el avance de esta
tienda, que necesitaba salir andando ya, así que **se revirtió**: este repo
volvió al estado de antes del SaaS (una sola tienda, sin `Modules`/
`PlanService`/tenant-scoping — ver el tag `saas-work-snapshot` si hace falta
recuperar algo). El trabajo del SaaS no se perdió, **se dividió en dos
proyectos nuevos y separados**, cada uno con su propio repo:

- **`C:\proyectos\saas`** (back `saasweb` + front) — la plataforma
  multi-tenant, sigue su desarrollo aparte.
- **`C:\proyectos\punto-de-venta`** (back + front) — módulo de Punto de
  Venta/Kiosco, también aparte, para no mezclarlo ni con el ecommerce de una
  sola tienda ni con el SaaS. Por ahora sólo está seedeado con un clone
  completo de este código; falta recortarlo a sólo lo que hace falta para POS.

Este repo (`backend-ecommer-ruth`) es y sigue siendo el ecommerce de **una
sola tienda** (la de la dueña). Regla para lo que se agregue de acá en más:
pensar cada feature nueva para esta tienda sola primero, pero de forma que
sea razonablemente migrable al SaaS más adelante si hace falta — **sin
construir multi-tenancy ahora**.

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
| Boilerplate | getters/setters/constructores a mano (sin Lombok) | evita el problema de Lombok + annotation processing en cada IDE |
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
| **Permission** | enum, 19 valores | Ver `model/Permission.java`. El más nuevo: `SHIFTS_MANAGE` (turnos, ver §12 #25) — lo tiene "Administrador" y "Vendedor" (se sumó a mano en la DB real porque `DataSeeder.ensureRole` no retocaba roles ya existentes). `PAYMENTS_MANAGE` (medios de pago, lo tiene "Administrador") y `PLATFORM_SETTINGS_MANAGE` (identidad/logo/WhatsApp/redes/textos/carrusel/servicio de mail — **sólo** lo tiene "Superadmin") reemplazaron a `SETTINGS_MANAGE`, que se sacó. |
| **PlatformMailSettings** | fila única `id='config'`, `host, port, username, password, fromAddress` | Credenciales SMTP (hoy Brevo). Editable sólo por superadmin (`PLATFORM_SETTINGS_MANAGE`) desde `/admin/config/servicios`, `GET/PUT /api/admin/platform/mail`. El `GET` nunca devuelve la clave en texto plano (`passwordSet: boolean`). Semilla inicial desde `app.mail.*` / env vars `BREVO_SMTP_*`. |
| **MarketingConfig** | fila única `id='config'`, `enabled, discountPercent, inactivityDays, spendThreshold, dailyEmailCap, couponValidityDays, cooldownDays, emailSubject?, emailBody?, emailImageUrl?` | Config de la campaña automática de cupón por email (ver §12 #23/#24). `email*` admiten los tokens `{tienda}`/`{codigo}`/`{porcentaje}`/`{vencimiento}`; null/vacío = texto por defecto. `emailImageUrl` = data URI (imagen arriba del mail). |
| **MarketingSend** | `id, email, reason (INACTIVE\|VIP), couponCode?, sentAt, status (SENT\|FAILED), errorMessage?, lifetimeSpendSnapshot?, lastOrderAtSnapshot?` | Log de cada envío (o intento) de campaña — historial en `/admin/campanias` + export CSV. Sólo cuenta para el **cooldown** (no repetirle a un mismo cliente) si `status=SENT`; un `FAILED` no bloquea el reintento al otro día. Los que quedan afuera por el **tope diario** ni siquiera generan fila acá, así que al otro día vuelven a entrar en la cuenta (no se pierden). |
| **SiteSettings** | fila única `id='config'`, `storeName, whatsappNumber, aboutText?, instagram?, facebookUrl?, logoUrl?, whatsappIntro?, whatsappClosing?, storeAddress?, payment{Transfer,QrTransfer,QrCard}Enabled, paymentTransferAlias?, paymentQr{Transfer,Card}Image?, paymentCardLink?, paymentCashEnabled` | Datos del local editables desde el panel. `whatsappNumber` valida `\d{8,15}`. `instagram` sin `@`. `logoUrl`/`paymentQr*Image` = data URI (MEDIUMTEXT). `whatsappIntro`/`whatsappClosing` = saludo/cierre (tokens `{tienda}`/`{codigo}`; el mensaje se arma en el front). `storeAddress` = dirección para el retiro. Cada medio de pago tiene su `*Enabled` (bool) aparte del dato → se ofrece si está habilitado **y** tiene el dato (efectivo sólo el bool). |
| **Product** | `id, name, description, price, ageRange, active, discontinued, createdAt, sizeScaleId?, supplierId?, costPrice?, lowStockThreshold?` | `supplierId`/`costPrice` = info interna, **no** se exponen en el catálogo público — desde §12 #35, `ProductDtos` tiene un DTO separado para admin (`ProductResponse`, con todo) y dos públicos (`PublicProductResponse`/`PublicProductListResponse`, sin estos dos campos). `lowStockThreshold` (unidades por talle) = umbral propio para la alerta de reposición; null = default global (`DashboardService.DEFAULT_LOW_STOCK` = 3). `discontinued` = "no reponer": sigue publicado/vendible pero `DashboardService.lowStock()` lo excluye. |
| — `images` | `List<String>` ordenada (`@ElementCollection` → `product_image`) | fotos, URL o data URI. `idx 0` = portada. En el DTO de admin y en el detalle público va como `images[]` + `imageUrl`; el **listado** público (`GET /api/products`) sólo trae `imageUrl` (§12 #35 — el array completo no se usa en las tarjetas). El request pide `images` (`@NotEmpty`). |
| — `sizeStocks` | `List<SizeStock{size, stock}>` (`@ElementCollection` → `product_size_stock`) | stock por talle |
| — `params` | `Set<ProductParam{groupId, optionId}>` (`@ElementCollection` → `product_param`) | en el DTO se expone como `Map<String,List<String>>` |
| **ParamGroup** | `id, name, multiple, showInCatalog, system` + `@OneToMany options` | grupos de clasificación (Público / Tipo / Estación). `system` = no se puede borrar. |
| **ParamOption** | `id, label, position` | |
| **SizeScale** | `id, name, system` + `values: List<String>` ordenada | escalas de talle (ropa bebé/niños/adultos, calzado) |
| **Supplier** | `id, name, phone?, address?, notes?` | proveedores del local |
| **Discount** | `id, kind (MONTO\|PARAMETRO\|PAGO\|ENVIO_GRATIS), discountPercent, enabled, stackable, label?, detail?, startsAt?, endsAt?, minAmount?, groupId?, optionId?, paymentMethods?` | `stackable` = acumulable. `detail` = letra chica configurable. `paymentMethods` = CSV de `PaymentMethod` (kind PAGO). `minAmount` sirve para MONTO y ENVIO_GRATIS. `activeNow()` = enabled + rango. DTO expone `status`. **Ya no existe `DiscountConfig`/combineMode**. |
| **Order** | `id, number (unique), customerName, customerEmail? (2026-09-11, opcional, no bloquea la venta — para la campaña de marketing y la base de clientes, ver §12 #23), subtotal, discountPercent, discountAmount, total, status (PENDIENTE\|PROCESADO\|CANCELADO), deliveryMethod (PICKUP\|SHIPPING), shippingAddress?, shippingReference?, shippingLat?, shippingLng?, paymentMethod? (TRANSFER\|QR_TRANSFER\|QR_CARD\|CASH), createdAt, processedAt?, createdByDni?/createdByName?, confirmedByDni?/confirmedByName?` | `code` = `"PED-" + %04d(number)` (getter `@Transient`). `number` = `MAX(number)+1`. `deliveryMethod` default PICKUP (pedidos viejos). El envío **no** se cotiza: no hay costo en el `Order`, se coordina aparte. `OrderService.create` exige `shippingAddress` si `deliveryMethod=SHIPPING`. `createdBy*`/`confirmedBy*` (2026-09-11, ver §12 #25) = quién armó / quién cobró — snapshots del nombre por DNI, null en pedidos viejos o del checkout web. |
| — `lines` | `List<OrderLine{id, productId, productName, size, quantity, unitPrice, accepted}>` | `productName` se guarda por si el producto cambia después |
| **Exchange** | `id, createdAt, processedByDni?/processedByName?` + `lines: List<ExchangeLine{DEVUELTA\|LLEVADA}>` | Cambios de prenda (ver §12 #21). `processedBy*` (2026-09-11, §12 #25) = snapshot de quién lo procesó, null en cambios viejos. |
| **Shift** | `id, userDni, userName (snapshot), openedAt, closedAt?` | Turno de un vendedor/cajero (2026-09-11, ver §12 #25). `closedAt=null` = turno abierto. Un usuario no puede tener dos abiertos a la vez. |
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
- Proyecciones DTO en el service y desactivar OSIV. (Parcial: el listado
  público de productos ya no dispara el N+1 de `images`/`sizeStocks`/`params`
  — §12 #35 — pero el resto de los endpoints con colecciones lazy sigue
  dependiendo de OSIV igual que antes.)
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
- ~~Frontend de Mercado Pago~~ — hecho (§12 #29).
- **Mercado Pago: falta probar con credenciales reales.** Lo hecho en #27/#29
  compiló y se probó en vivo forzando un error real (token inválido a
  propósito, para confirmar que el pedido no se pierde) — falta un Access
  Token de prueba/producción real cargado desde el panel, y un túnel (ngrok)
  en local para que el webhook sea alcanzable.
- ~~Frontend de Gastos/Balance/movimientos de stock~~ — hecho (§12 #31).
- **Probar Gastos/Balance/movimientos de stock en el navegador** — sólo se
  verificó por código + `ng build` (la extensión de Chrome estaba
  desconectada esa sesión), no en uso real.
- **`punto-de-venta`** (`C:\proyectos\punto-de-venta`, ver §1): sólo tiene un
  clone completo de este código como semilla — falta recortarlo a sólo el
  módulo POS/Kiosco.
- ~~Alertas de stock bajo por mail~~ — hecho (§12 #32).
- ~~Código de barras interno por producto~~ — hecho (§12 #33).
- ~~Recuperación de contraseña con link~~ — hecho (§12 #34).
- **No abordado todavía** (evaluar si la dueña lo necesita — existe en el
  SaaS, no se portó): ARCA/facturación electrónica + Notas de Crédito.

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
25. **QR por producto + quién vendió/cobró + turnos con cierre de caja
    (2026-09-11, misma sesión que #22-24, tanda siguiente).**
    - **Quién armó/cobró la venta**: `Order` sumó `createdByDni`/`createdByName`
      (se completan en `OrderService.createPos`, null en el checkout web
      público) y `confirmedByDni`/`confirmedByName` (se completan recién en
      `confirm`, sea el mismo pedido del POS o uno del checkout web).
      `Exchange` sumó el mismo par `processedByDni`/`processedByName`. Son
      **snapshots** del nombre (mismo criterio que `OrderLine.productName`):
      si el usuario cambia de nombre o se borra después, el pedido/cambio
      viejo no se rompe. `OrderController`/`ExchangeController` resuelven el
      DNI de `Authentication.getName()` (JWT). CSV de pedidos/cambios sumó
      columnas "Vendió"/"Cobró"/"Proceso".
    - **POS en dos pasos**: `OrderService.createPos` **dejó de confirmar
      automáticamente** — crea el pedido `LOCAL`/`PENDIENTE` con
      `createdBy*` seteado; `confirm` (el de siempre, ahora con `confirmedBy*`)
      pasa a ser el paso de "cobrar", ya sea desde el POS en el momento (caso
      normal, un solo empleado) o después desde `/admin/pedidos` (si lo cobra
      otra persona). Sin endpoint nuevo — es el mismo `POST .../confirm` de
      siempre.
    - **Turnos** (`Shift`, nueva entidad + `ShiftService` + `ShiftController`
      bajo `/api/admin/shifts`, permiso nuevo `SHIFTS_MANAGE`): `GET /current`,
      `POST /open` (400 si ya hay uno abierto), `POST /{id}/close` (400 si ya
      está cerrado; lo puede cerrar el dueño del turno o alguien con
      `CASH_REGISTER_VIEW`, para turnos olvidados), `GET` paginado (`userDni`
      opcional). `CashRegisterService.forShift(shift)` — refactor: `forDay` y
      `forShift` comparten un `build(label, orders, exchanges)` privado;
      `forShift` filtra pedidos por `confirmedByDni == shift.userDni` y
      cambios por `processedByDni == shift.userDni` en la ventana
      `[openedAt, closedAt ?? now)`, así el cierre de caja de un turno es sólo
      lo que **esa persona** cobró/procesó, no todo lo del local.
      `GET /api/admin/cash-register/shift/{id}` nuevo (reusa `CashRegisterResponse`,
      el campo `date` se repurpone como label genérico "Turno de {nombre}").
    - **QR por producto**: sin cambios de backend — el QR sólo codifica la URL
      pública del producto (`{origin}/producto/{id}`, ruta que ya existe), 100%
      frontend (`qrcode` para generarlo, `@zxing/browser` para leerlo con la
      cámara).
    - `Permission.SHIFTS_MANAGE` sumado a `DataSeeder` para "Administrador"/
      "Vendedor" — **no** retroactivo (`ensureRole` sólo siembra roles que no
      existen), así que se agregó a mano en la DB real
      (`INSERT INTO role_permission ...`) para esos dos roles ya existentes.
    - `schema.sql` actualizado: columnas nuevas en `orders`/`exchange`, tabla
      `shift` nueva. Sin migración (`ddl-auto=update`).
    - Probado en vivo: turno abierto → venta en el local dejada pendiente →
      confirmada desde `/admin/pedidos` (mismo usuario en esta prueba, por
      falta de una segunda cuenta a mano) → caja del turno reflejó el total →
      turno cerrado. QR de un producto impreso. Todo revertido/limpiado de la
      base real después (pedido, línea, turno borrados; stock restaurado).
26. **Merge de las ramas `develop` (local) y `origin/develop` (2026-09-12):**
    `origin/develop` traía trabajo en paralelo (2026-09-10/11) que esta rama
    nunca había bajado: Cloudinary editable desde el panel (`site_settings`
    sumó `cloudinaryCloudName`/`cloudinaryUploadPreset`, endpoints
    `GET`/`PUT /api/admin/settings/cloudinary`), un flag `AdminUser.superAdmin`
    (boolean) separado del sistema de roles (con authority `SUPERADMIN` en el
    JWT vía `JwtAuthFilter`), y un scaffolding de SMTP en `site_settings`
    (`smtp*`) con pantalla `/admin/superadmin/mail`. Se sacó Lombok del
    proyecto (`pom.xml` ya no lo tiene como dependencia) y se pasó a getters/
    setters a mano.
    - **Se quedaron los dos ejes de "superadmin":** el rol de sistema
      ("Superadmin", ver #22) sigue siendo el único que tiene todos los
      `Permission`; se sumó `AdminUser.superAdmin` como eje aparte, ungrantable
      desde `/admin/usuarios` (a diferencia de un `Permission`, no hay forma de
      dárselo a un rol normal por la UI) — sirve específicamente para
      Cloudinary y el mail SMTP, que son secretos de infraestructura, no
      config de negocio. `AuthService.ensureInitialSuperadmin()` (siembra por
      `app.superadmin.*`, ver #22) ahora setea los dos ejes en la misma cuenta.
    - **4 modelos usaban Lombok** (`MarketingConfig`, `MarketingSend`,
      `PlatformMailSettings`, `Shift`, todos de #23-25) y quedaron rotos al
      sacar la dependencia — se les escribieron los getters/setters a mano.
    - **Pendiente de decidir (no se tocó en este merge):** quedaron **dos
      configuraciones de SMTP independientes** — `PlatformMailSettings` (la
      que de verdad usan `AccountMailService`/`MarketingMailService` para
      mandar mail, ver #24, editable en `/admin/config/servicios`) y los
      campos `smtp*` de `SiteSettings` (editables en `/admin/superadmin/mail`,
      gateados por `SUPERADMIN`, pero sin conectar a ningún envío real). Falta
      decidir si se unifican (lo más simple: apuntar `/admin/superadmin/mail`
      a `PlatformMailSettings` y borrar los campos `smtp*` de `SiteSettings`)
      o si se les da un propósito distinto a cada una.
    - `database/schema.sql`: se agregó la columna `super_admin` a `admin_user`
      (faltaba). Los campos `cloudinary*`/`smtp*` de `site_settings` siguen
      sin reflejarse en `schema.sql` (existían así desde antes del merge, se
      crean solos con `ddl-auto=update`).

27. **Checkout online con Mercado Pago (2026-09-17):** el carrito ahora puede
    pagar de verdad con Mercado Pago (Checkout Pro), sin nada de multi-tenant
    ni gating por plan — este proyecto volvió a ser el ecommerce de una sola
    tienda antes de que arrancara el desvío hacia el SaaS (ver el tag
    `saas-work-snapshot` si hace falta recuperar algo de esa rama, que ahora
    vive aparte en otro repo).
    - `PaymentMethod` suma `MERCADOPAGO`. `Order` suma `paymentStatus`
      (`PaymentStatus`: PENDING/APPROVED/REJECTED — sólo tiene sentido para
      `MERCADOPAGO`, el resto de los medios de pago sigue sin ningún estado de
      pago online), `mpPreferenceId`, `mpCheckoutUrl`, `mpPaymentId`.
    - `MercadoPagoService` (nuevo, en `service/`): cliente de la API de MP —
      crea la preferencia de Checkout Pro y consulta un pago por id.
    - `OrderService.createWebCheckout` reemplaza a `create` como entrada del
      checkout público (`OrderController.create` la usa): si el medio es
      `MERCADOPAGO`, crea la preferencia (`startMercadoPagoCheckout`) y guarda
      `mpCheckoutUrl`. `confirm`/`confirmFromPayment` ahora comparten la lógica
      de descuento de stock vía un `doConfirm` privado — `confirmFromPayment`
      la dispara el webhook cuando MP aprueba el pago (sin DNI de un humano) y
      manda el mail de confirmación (`OrderMailService`, nuevo, mismo patrón
      que `AccountMailService`: usa el SMTP de `PlatformMailSettingsService`).
      `markPaymentRejected` cancela el pedido si MP lo rechaza — nunca se tocó
      stock hasta confirmar, así que cancelar es siempre seguro.
    - `MercadoPagoWebhookController` (nuevo, público, sin JWT): recibe la
      notificación de MP, pero **nunca confía en su body** — vuelve a
      consultar el pago real a la API con el Access Token guardado, y sólo
      ahí decide confirmar o rechazar. Siempre responde 200 (MP reintenta
      agresivo si no; los casos que fallan quedan sólo logueados).
    - Credenciales en `SiteSettings`: `mpEnabled`, `mpAccessToken` (secreto,
      nunca se devuelve en ninguna respuesta — mismo criterio que
      `smtpPassword`), `mpPublicKey`. Nuevo endpoint
      `GET/PUT /api/admin/settings/mercadopago`, gateado por `PAYMENTS_MANAGE`
      (es la cuenta del propio dueño de la tienda, no algo de superadmin).
      `SettingsResponse` suma `mercadoPagoAvailable` (true sólo si está
      habilitado y ya tiene Access Token cargado).
    - `AppProperties`/`application.yml` suman `app.urls.backend`/`frontend`
      (`BACKEND_PUBLIC_URL`/`FRONTEND_URL`) — hacen falta para armar
      `notification_url`/`back_urls` de la preferencia de MP.
    - `SecurityConfig`: `POST /api/webhooks/mercadopago` público (MP no manda
      ninguna sesión nuestra).
    - `database/schema.sql` y `database/setup.sql` actualizados con las
      columnas nuevas de `orders` y `site_settings`.
    - **Mercado Pago es excluyente en la venta online:** si `mpEnabled=true`,
      `createWebCheckout` rechaza cualquier `paymentMethod` que no sea
      `MERCADOPAGO` ("Esta tienda solo acepta Mercado Pago como medio de pago
      online") — no tiene sentido ofrecer transferencia/QR a la vez que
      Checkout Pro real. Si `mpEnabled=false`, rechaza `MERCADOPAGO` (todavía
      no está configurado). El frontend tiene que reflejar esto: si
      `mercadoPagoAvailable=true`, mostrar sólo "Pagar con Mercado Pago" en el
      carrito y ocultar transferencia/QR/efectivo para la venta online.
    - **Mercado Pago NO existe en la venta del local:** `createPos` rechaza
      `MERCADOPAGO` siempre — ahí se sigue cobrando en efectivo, transferencia
      o posnet, nunca con el checkout online.
    - **Falta probar con token real:** todo esto compiló pero no se probó
      contra la API real de Mercado Pago (hace falta un Access Token de
      prueba/producción cargado desde el panel, y un túnel tipo ngrok en
      local para que el webhook sea alcanzable). Pendiente además: portar el
      lado del frontend (pantalla de "Medios de pago" para cargar el Access
      Token, y el checkout redirigiendo a `mpCheckoutUrl`).

28. **Costeo por compras + Gastos y Balance (2026-09-18):** portado desde el
    proyecto SaaS, sin nada de multi-tenant ni gating por plan. Método de
    costeo elegido explícitamente: **promedio ponderado**, no lotes/FIFO — el
    stock viejo y el nuevo se mezclan en un solo `Product.costPrice`, no se
    mantienen separados.
    - **Costo histórico congelado:** `OrderLine.costPrice` se llena recién en
      `doConfirm` (mismo momento en que se descuenta stock), leyendo el
      `Product.costPrice` de ESE momento. Si más adelante cambia el costo del
      producto, el margen de pedidos ya confirmados no se recalcula solo.
      `null` = el producto no tenía costo cargado cuando se vendió.
    - **`ProductService.registerPurchase`** (nuevo): compra a proveedor, suma
      stock y recalcula `Product.costPrice` = promedio ponderado entre el
      stock que ya había (a su costo actual) y lo que entra (a su costo de
      compra). Si el producto no tenía costo, el costo pasa a ser directo el
      de esta compra. Endpoint `POST /api/admin/products/{id}/purchase`
      (`STOCK_MOVEMENTS_VIEW`).
    - **`StockMovement`** (nuevo, tabla `stock_movement`): una fila por cada
      cambio de stock (motivo, referencia, costo unitario si es compra),
      instrumentado en el único punto de entrada real de `ProductService`
      (`decrementStock`/`incrementStock`/`setStock`, que cambiaron de firma —
      ver `OrderService.doConfirm` y `ExchangeService.create` para los
      motivos `VENTA`/`CAMBIO_DEVUELTA`/`CAMBIO_LLEVADA`). Listado con
      filtros: `GET /api/admin/stock-movements` (`STOCK_MOVEMENTS_VIEW`).
    - **`MetricsDtos.Totals`** suma `cost`/`costDataComplete` (costo = suma de
      `OrderLine.costPrice × cantidad` de las líneas que lo tienen cargado;
      `costDataComplete=false` si alguna línea contabilizada no lo tenía).
      Se propaga a los 3 `Totals` que arma `MetricsService` (total, web,
      local) y al nuevo `MetricsService.rangeTotals(from, to)` que usa
      Balance. **Nota de alcance:** `ExchangeLine` NO congela costo (el SaaS
      tampoco lo usaba para el cálculo de margen, sólo `OrderLine`) — el
      margen no contempla la diferencia cobrada en cambios de prenda.
    - **Módulo de Gastos y Balance** (nuevo, `Expense`/`ExpenseBudget`/
      `BalanceService`): `Expense` con recurrencia mensual opcional
      (`ExpenseRecurrenceScheduler`, 1° de cada mes 04:00) genera sola la
      instancia del mes siguiente de cada serie "repetir cada mes".
      `ExpenseBudget`: presupuesto fijo por categoría, comparado contra el
      gasto real del mes (`GET /api/admin/expense-budgets/status`).
      `BalanceService`: ventas − costo − gastos, con comparativa mensual del
      año (`GET /api/admin/balance`, `GET /api/admin/balance/comparison`).
      `BalanceService` en sí no depende de si se factura o no, calcula sobre
      pedidos `PROCESADO` nomás.
    - **3 permisos nuevos:** `STOCK_MOVEMENTS_VIEW`, `EXPENSES_MANAGE`,
      `FINANCE_VIEW`. Se le dan a "Administrador" en `DataSeeder`, con
      backfill (`RoleService.grantPermissionsIfMissing`) para instalaciones
      que ya tenían ese rol creado de antes — no le tocan otros permisos que
      el dueño haya destildado.
    - `database/schema.sql` suma `order_line.cost_price` y las tablas
      `stock_movement`/`expense`/`expense_budget`; `database/setup.sql`
      regenerado (`cat schema.sql seed.sql > setup.sql`) — de paso quedó al
      día con varias fases anteriores que habían quedado desactualizadas ahí
      (no afecta nada: `ddl-auto=update` crea el esquema real solo, este
      archivo es sólo referencia para instalación manual).
    - **Pendiente:** portar el lado del frontend (pantallas de Gastos,
      Balance, movimientos de stock/registrar compra) — queda para otra
      tanda. No se probó en navegador.

29. **Frontend de Mercado Pago + bug transaccional real encontrado y arreglado
    (2026-09-17, misma sesión que #27):**
    - Port del lado del frontend: `PaymentMethod`/`PaymentStatus` nuevos,
      botón de pago del carrito condicional ("Pagar con Mercado Pago" en vez
      de "Comprar por WhatsApp" cuando `mercadoPagoAvailable=true`), redirect
      a `mpCheckoutUrl` tras crear el pedido, botón "Reintentar pago" en Mis
      Pedidos si quedó `PENDING`, pantalla nueva en Configuración → Pagos
      (toggle + Access Token + Public Key, mismo patrón "secreto que no se
      re-muestra" que el resto del panel).
    - **Probado en vivo**, end-to-end, contra la API real de Mercado Pago (con
      un Access Token de prueba inválido a propósito, para forzar el 403
      real). Eso destapó un bug real que nadie había pedido investigar:
      `createWebCheckout` perdía **el pedido entero** (no sólo el intento de
      pago) cuando `startMercadoPagoCheckout` fallaba, porque la clase es
      `@Transactional` y el rollback por defecto de Spring deshacía también
      el `create()` ya persistido en la misma transacción. Fix:
      `@Transactional(noRollbackFor = BadRequestException.class)` en
      `createWebCheckout` — verificado repitiendo la misma prueba: el pedido
      ahora queda `PENDIENTE` en la base en vez de desaparecer.

30. **POS: cartel grande de vuelto al pagar en efectivo (2026-09-17/18):**
    - Sin cambios de backend. `AdminPosComponent` (frontend) suma un input
      "Efectivo recibido" y un cartel grande (verde "Vuelto" / rojo "Falta")
      debajo del total, visible sólo con medio de pago CASH — para que el
      vendedor no se confunda al dar el cambio.

31. **Frontend de Costeo/Gastos/Balance + categoría de gasto editable
    (2026-09-18, continuación de #28):**
    - Port de las 3 pantallas que faltaban de #28: `admin-expenses`
      (`/admin/gastos`, alta/edición de gastos + presupuesto mensual por
      categoría con alerta), `admin-balance` (`/admin/balance`, totales del
      período + comparativa mensual/anual), `admin-stock-movements`
      (`/admin/movimientos-stock`, ajuste manual + registrar compra a
      proveedor con recálculo de costo + historial filtrable). Rutas y menú
      gateados por los 3 permisos de #28 (mismo patrón que el resto del
      panel). Verificado con `ng build`; **no probado en navegador**
      (extensión de Chrome desconectada esa sesión).
    - **Categoría de gasto, de lista fija a parametría editable:** al portar
      lo anterior quedó una laguna — el SaaS resolvía "categoría de gasto"
      con una parametría (`ParamGroup`/`ParamOption`), pero acá nunca se
      sembró, así que las categorías quedaron hardcodeadas en el frontend
      (`EXPENSE_CATEGORIES`), sin forma de agregar una nueva sin tocar
      código. Corregido: en vez de duplicar el motor de parametrías con una
      entidad `ExpenseCategory` nueva (el motor genérico ya existe en este
      backend desde antes, para Público/Tipo de prenda/Estación), se siembra
      "Categoría de gasto" como **un `ParamGroup` más**
      (`grp-categoria-gasto`, `system=true` así no se borra el grupo entero,
      `showInCatalog=false` porque no es un filtro de catálogo) —
      `DataSeeder.seedExpenseCategoryParamGroup()`, corre en cada arranque
      (no sólo en instalaciones nuevas) con las mismas 8 categorías/ids que
      ya venía usando el frontend, para no romper `categoryOptionId` de
      gastos ya cargados. El dueño ya puede agregar/editar/borrar categorías
      desde `/admin/parametrias`, la misma pantalla de siempre.
      `admin-expenses` pasó a leer del `ParamService` en vez de la lista fija.
      Probado en vivo: backend levantado, `/api/param-groups` devolviendo el
      grupo nuevo con las 8 opciones.
    - **Fix de paso:** `seedParamGroups()` chequeaba `paramRepo.count() > 0`
      para no re-seedear — pero como `seedExpenseCategoryParamGroup()` corre
      antes y sin depender de `SEED_ENABLED`, en una base nueva ese count()
      ya daba >0 y Público/Tipo de prenda/Estación no se cargaban nunca
      (lo agarró `CatalogTest`). Cambiado a chequear el id puntual
      `grp-publico`.

32. **Alertas de stock bajo por mail (2026-09-18, portado del SaaS):**
    - `SiteSettings` suma `lowStockAlertEnabled`/`lowStockAlertEmail`,
      editable por `PRODUCTS_MANAGE` en `GET`/`PUT
      /api/admin/settings/stock-alert`. Pantalla nueva en Configuración.
    - `LowStockAlertScheduler` (cron `0 0 7 * * *`, antes de que abra el
      local) no manda nada si no está activada o no tiene mail cargado;
      reusa `DashboardService.lowStock()`. `LowStockAlertMailService` arma
      un mail HTML con la tabla de talles. Sin tenant/plan (a diferencia del
      SaaS, acá es la única tienda, no hay loop de tenants).

33. **Código de barras interno por producto, opcional (2026-09-18, portado
    del SaaS):**
    - `Product.barcode` (columna nueva, `unique`, nullable): alternativa/
      complemento al QR, que sigue existiendo siempre sin cargar nada. Se
      puede tipear el código real del fabricante o generar uno interno
      (`"IN" + dígitos del id`) desde la ficha del producto —
      `POST /api/admin/products/{id}/generate-barcode` (`PRODUCTS_MANAGE`),
      no pisa uno ya cargado. `GET /api/admin/products/by-barcode?code=`
      (`PRODUCTS_VIEW`) para buscar por código. `duplicate()` no copia el
      barcode del original (evitaría chocar contra el `unique`).
    - Frontend: campo + botón "Generar código interno" + link a una
      etiqueta imprimible (`jsbarcode`, mismo patrón que el QR con
      `qrcode`) en la ficha del producto; columna "Código" en el listado
      cuando el producto tiene uno. El buscador de `admin-pos` también
      matchea por barcode completo, para que un lector USB (que sólo
      "tipea" el código en el foco actual) funcione sin cablear nada nuevo
      — el SaaS nunca llegó a integrar esto último, quedó sólo como
      plumbing de backend sin consumidor en el frontend.

34. **Recuperación de contraseña con link, en vez de mandar una clave nueva
    (2026-09-18, portado del SaaS):**
    - `AdminUser` suma `resetTokenHash` (SHA-256 del token, nunca el token
      en sí — mismo criterio que la contraseña) + `resetTokenExpiresAt`.
      `AuthService.forgotPassword` genera un token de 32 bytes, lo guarda
      hasheado (vence en 1 hora) y arma el link a
      `{frontend}/admin/restablecer-clave?token=`. `resetPassword` (nuevo,
      `POST /api/auth/reset-password`, público) valida que exista y no haya
      vencido, lo borra en el acto (de un solo uso) y recién ahí cambia la
      contraseña. Se sacó `sendTempPassword`/sigue existiendo
      `AccountMailService`, ahora con `sendPasswordResetLink`.
    - **Bug real encontrado probándolo en vivo** (mismo patrón que el de
      Mercado Pago del §12 #29): el token se guarda ANTES de mandar el
      mail (tiene que existir para poder armar el link), y como
      `AuthService` es `@Transactional` a nivel de clase, una falla al
      mandar el mail deshacía también el guardado del token — y de paso
      convertía la respuesta en un 500 sólo para DNIs que sí existen,
      filtrando esa existencia (rompe el "no revela si el DNI existe" que
      es el punto del endpoint). Solucionado atajando la excepción del mail
      sender ahí mismo (mismo patrón que `OrderMailService`): el token
      queda guardado y la respuesta sigue siendo 204 pase lo que pase con
      el envío.
    - Probado en vivo end-to-end (sin SMTP real en este entorno): token
      guardado en la base pese a que el envío del mail falló, reset con el
      token real funcionó, login con la contraseña nueva funcionó, y
      reusar el mismo token dio 400 "venció o ya se usó" (de un solo uso
      confirmado).
    - Frontend: pantalla nueva `admin-reset-password`
      (`/admin/restablecer-clave?token=`) para elegir la contraseña;
      `admin-recover` actualiza su copy ("te mandamos un link" en vez de
      "te mandamos una contraseña nueva") — el pedido en sí no cambió de
      forma, sigue siendo sólo el DNI.

35. **Auditoría de over-fetching/N+1 en el catálogo público (2026-09-20):**
    - **Filtración de datos internos (seguridad, no sólo performance):**
      `GET /api/products` y `GET /api/products/{id}` (sin auth) devolvían el
      mismo DTO que el admin, exponiendo `costPrice`/`supplierId` — que
      §5 dice explícitamente que no se exponen en el catálogo público.
      `ProductDtos` ahora separa tres respuestas: `ProductResponse` (admin,
      igual que antes), `PublicProductResponse` (detalle público: sin
      `costPrice`/`supplierId`, con la galería completa) y
      `PublicProductListResponse` (listado público: además sin `images[]`
      completo). `ProductController` usa cada una donde corresponde;
      `/api/products/best-sellers` (también público) se corrigió igual.
    - **`images[]` fuera del listado:** medido con curl, el array `images[]`
      era ~45% del payload de `GET /api/products` (21.2 KB → 11.6 KB con 11
      productos de prueba) y no se usa ahí (`product-card.component.html`
      sólo lee `imageUrl`). Se sacó del DTO de listado; el de detalle
      (`GET /api/products/{id}`) lo sigue trayendo completo (la ficha de
      producto sí muestra la galería). `sizeStocks`/`params` se mantuvieron
      en el listado — el catálogo los usa client-side para filtrar por talle
      y por parametría (`catalog-page.component.ts`), y `product-card`
      calcula el stock total con `sizeStocks`.
    - **Frontend (parity):** `product-detail-page` armaba la galería leyendo
      `product().images` de la lista pública cacheada — nunca llamaba al
      detalle. Al sacar `images[]` del listado eso hubiera roto la galería
      de la ficha de producto. Se agregó `ProductService.fetchOnePublic(id)`
      (`GET /api/products/{id}`) y `product-detail-page` ahora pide la
      galería completa aparte cuando cambia el `:id` de la ruta
      (`detailImages`, ver constructor del componente).
    - **N+1 al listar:** `Product.images/sizeStocks/params` son
      `@ElementCollection` lazy; con `open-in-view` se resolvían fila por
      fila. Para `findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc`
      (listado público + `DashboardService.lowStock()`) se agregó
      `@EntityGraph(attributePaths = {"sizeStocks", "params"})` — las trae
      en la misma consulta en vez de una por producto. `images` no entra en
      ese entity graph (ninguno de los dos casos la necesita); la portada
      del listado se resuelve aparte con `ProductRepository.findCoverImages`
      (una sola consulta JPQL con `index(img) = 0` para todos los ids de la
      página, en vez de disparar la colección completa por producto sólo
      para leer `images.get(0)`). No se tocó el detalle ni los endpoints de
      admin (siguen trayendo todo, sin restricción de columnas).
    - **Gzip** (`server.compression`, umbral 1 KB) y **`Cache-Control:
      public, max-age=300`** en `GET /api/param-groups`, `/api/size-scales`
      y `/api/settings` (datos casi estáticos) — se cambió su firma de
      `List<...>`/DTO plano a `ResponseEntity<...>` para poder setear el
      header sin tocar el JSON de respuesta.
    - **Pool de Hikari** acotado a `maximum-pool-size: 5` (antes sin límite
      explícito = default de 10) — este backend es de un solo comercio, no
      necesita más.
    - Verificado con curl: catálogo público sin `costPrice`/`supplierId` ni
      `images[]`, detalle con todo salvo los dos campos internos, admin sin
      cambios, `Content-Encoding: gzip` presente, `Cache-Control` presente
      en los 3 endpoints casi-estáticos, y `hikaricp.connections.max=5` vía
      `/actuator/metrics`. 29 tests en verde (los 28 existentes + uno nuevo
      que cubre el detalle público).
    - **Pendiente** (no abordado en esta tanda, ver §11): `open-in-view`
      sigue activado y el resto del service sigue mapeando entidades a DTO
      sin proyecciones — esta tanda resolvió puntualmente el listado
      público, no el resto de los endpoints con colecciones lazy.

---

## 13. Despliegue a producción — Hetzner (guía paso a paso)

Checklist para el día que se pague y active el server real (hoy: local +
Cloudinary de prueba, sin pagar nada de Hetzner todavía — ver §11 "Perfil
prod" y "Subida de imágenes"). Pensado para retomarlo con SSH/PuTTY
oxidado — no da nada por sabido.

### 13.1 Variables de entorno que SÍ O SÍ hay que cambiar (no dejar el default de dev)

| Var | Default dev (no usar en prod) | Qué poner |
|---|---|---|
| `DB_USER`/`DB_PASSWORD` | `root`/`root` | usuario MySQL dedicado, no root |
| `JWT_SECRET` | placeholder de dev | random ≥32 chars, ej. `openssl rand -base64 32` |
| `ADMIN_NOMBRE`/`_APELLIDO`/`_DNI`/`_EMAIL`/`_PASSWORD` | Ruth/.../`ruth123` | datos reales de la dueña, contraseña fuerte |
| `SUPERADMIN_NOMBRE`/`_APELLIDO`/`_DNI`/`_EMAIL`/`_PASSWORD` | Augusto/.../`augusto123` | contraseña fuerte propia |
| `BREVO_SMTP_HOST`/`_PORT`/`_USER`/`_KEY`, `MARKETING_FROM_EMAIL` | placeholders `changeme@...` | credenciales reales de Brevo (o el proveedor SMTP que se use) |
| `CORS_ORIGINS` | `localhost:4200,4300` | dominio real del frontend, `https://...` |
| `SEED_ENABLED` | `true` | **`false`** — no cargar datos de ejemplo en prod |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` (implícito) | `validate` o `none` (ver §9) |
| `SERVER_PORT` | `8080` | dejar así, nginx hace de proxy hacia afuera |

Falta crear `application-prod.yml` (pendiente, §11) para no tener que pasar
todo por env var a mano cada vez.

### 13.2 Storage de imágenes (Cloudinary / Hetzner Object Storage)

**Pendiente — feature todavía en diseño** (brainstorming en curso: Cloudinary
activo ahora, Hetzner Object Storage listo pero apagado hasta pagarlo). Acá
van a ir, cuando se cierre el diseño: credenciales de Cloudinary (API
key/secret, no solo el preset unsigned que tiene el SaaS — para poder borrar
imágenes también), y las de Hetzner S3 (endpoint, bucket, access key, secret
key) + el flag que elige cuál de las dos implementaciones está activa.

### 13.3 Crear el servidor en Hetzner

1. Cuenta en hetzner.com + tarjeta cargada.
2. Cloud Console → **New Server**.
3. Ubicación: Falkenstein/Nuremberg (Alemania, las más baratas) o Ashburn
   (EEUU) — probar ping a cada una antes de elegir, por la latencia a
   Argentina.
4. Imagen: **Debian 12** (mismo sistema que ya conocés del server de la
   empresa).
5. Tipo: **CX22** (2 vCPU / 4GB) para arrancar — se puede resizear después
   sin redeploy (ver conversación arriba).
6. SSH key: pegar la pública. Si no tenés una a mano, generarla con
   `ssh-keygen -t ed25519` (Git Bash o PowerShell, Windows ya trae el
   cliente SSH — no hace falta instalar PuTTY de nuevo). Con esto entrás sin
   contraseña.
7. Crear → te da una IP pública fija.

### 13.4 Conectarse

```
ssh root@<ip>
```
directo desde Git Bash o PowerShell.

### 13.5 Instalar lo necesario en el server

- `apt update && apt upgrade`
- Java 21: `apt install openjdk-21-jre-headless`
- MySQL: `apt install mysql-server` + `mysql_secure_installation` + crear DB
  y usuario dedicado (no root, ver 13.1)
- nginx: `apt install nginx`
- Firewall (`ufw`): permitir 22/80/443 nada más — el 8080 del backend queda
  **solo accesible desde localhost**, nginx es el único que le habla afuera.

### 13.6 Subir el código y correrlo

- Buildear el jar en tu máquina (`./mvnw package`) y subirlo por `scp` — más
  simple que clonar el repo entero y buildear en el server.
- Crear un `systemd` service (`/etc/systemd/system/backend-ruth.service`)
  que arranque el jar con las env vars de 13.1 y `Restart=on-failure`, para
  que sobreviva a un crash o a un reboot del server sin que haya que
  entrar a mano cada vez.

### 13.7 Base de datos

`mysql < database/setup.sql` — **pero ojo**: §9 avisa que este script tiene
drift viejo (le faltan columnas/tablas de tandas más recientes). Revisarlo
contra las entidades actuales del código **antes** de correrlo en un deploy
nuevo, si no el arranque con `ddl-auto=validate` va a fallar por columnas
faltantes.

### 13.8 Dominio + HTTPS

- El dominio lo paga y gestiona cada cliente (ya decidido).
- Apuntar el registro DNS tipo `A` del dominio (y del subdominio del admin,
  si va aparte, ej. `admin.dominio.com`) a la IP del server.
- nginx como reverse proxy: escucha 80/443 y redirige a `localhost:8080`.
- HTTPS gratis con Let's Encrypt: `apt install certbot python3-certbot-nginx`
  → `certbot --nginx -d dominio.com -d www.dominio.com`. Se autorenueva solo.

### 13.9 Checklist final antes de dar el deploy por terminado

- [ ] Login admin con las credenciales reales, no las de dev
- [ ] `SEED_ENABLED=false` confirmado (catálogo público sin productos de ejemplo)
- [ ] `/api/admin/**` rechaza requests sin token
- [ ] Mail de recuperación de contraseña probado con las credenciales SMTP reales
- [ ] Puerto 8080 bloqueado desde afuera por el firewall (solo nginx lo ve)
- [ ] Backup automático de la base (cron con `mysqldump` a un storage aparte de este mismo server)

Este resumen es para no perder nada en el medio — la primera vez lo hacemos
acompañado, paso a paso, no como reemplazo de eso.
