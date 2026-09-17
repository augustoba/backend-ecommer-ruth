# Plan de evolución a SaaS — Roadmap

> Documento vivo. Registra QUÉ se decidió, QUÉ se hizo y QUÉ queda pendiente
> del plan para evolucionar este e-commerce hacia una plataforma SaaS
> multi-tenant. Se actualiza cada vez que se avanza una fase (no día a día:
> para eso está el Historial de `PROYECTO.md`).

Última actualización: 2026-09-16.

Origen: propuesta inicial en `propuesta_ecommerce_saas.txt` (escritorio del
usuario) + análisis del código actual hecho en esta conversación.

---

## 1. Estrategia actual (IMPORTANTE — leer antes de tocar código)

**Por ahora seguimos con UN SOLO e-commerce.** Esta tienda (Estilos
Pequeños) es una **prueba real**: va a tener uso real, feedback real, y va
a cambiar con mejoras que surjan de ese uso. No se va a ofrecer a otros
clientes hasta que esté óptima.

**Actualización 2026-09-15:** al principio la idea era "no tocar nada de
multi-tenancy hasta tener un segundo cliente". Se ajustó: el usuario pidió
que el código se vaya adaptando a multi-tenant *desde ahora*, aunque exista
un solo tenant, para no escribir código hoy y tener que volver a tocarlo
mañana. El criterio para decidir qué hacer ahora es distinto al de antes:

- **SÍ hacer ahora** lo que es infraestructura genuinamente reusable y de
  bajo riesgo: cosas que no cambian comportamiento visible, no le complican
  la vida a la tienda actual, y evitan tener que volver a tocar el mismo
  código cuando exista un segundo tenant real (ver Fase 3).
- **Seguir difiriendo** lo que depende de decisiones de negocio que todavía
  no están tomadas (planes, precios, límites) o de tener un segundo tenant
  real para validar contra algo (resolución por dominio, aislamiento
  probado, themes, personalizador, dominios propios). Construir eso ahora
  sería diseñar contra requisitos hipotéticos — el problema que se quiere
  evitar es "código que se vuelve a tocar", no "trabajo especulativo".

**Regla general: la infraestructura de multi-tenancy se prepara ahora; las
decisiones de producto/negocio del multi-tenant se toman cuando haya un
segundo tenant real.**

---

## 2. Cambio de nombre del proyecto

Hoy el paquete base es `com.estilospequenos` y el nombre del proyecto es
"Estilos Pequeños" — tiene sentido para la tienda, no para la plataforma.

**Estado: hecho.** Nombre confirmado: `saasweb`.

Se distinguió deliberadamente dos identidades distintas:
- **Identidad de código/plataforma** → `saasweb` (paquete Java, `pom.xml`).
- **Identidad de marca/negocio de esta tienda** → sigue siendo "Estilos
  Pequeños" (nombre de la base de datos `estilos_pequenos`, dominio de mail
  `estilospequenos.com`, usuario de Instagram, preset de Cloudinary, textos
  seedeados). No tiene sentido tocar eso: es la tienda real en producción,
  y renombrar la DB es una operación de infraestructura aparte (no un
  rename de código) que no se justifica mientras haya un solo tenant.

Cambios realizados (2026-09-15):
- [x] Paquete Java `com.estilospequenos` → `com.saasweb` (main + test, con
      `git mv` para preservar historial)
- [x] `pom.xml`: `groupId` → `com.saasweb`, `<name>` → `SaasWeb - Backend`
- [x] `PROYECTO.md` / `README.md`: referencias al paquete base actualizadas
- [x] Verificado: compila y los 28 tests existentes pasan en verde
- [x] Dejado sin tocar a propósito: DB `estilos_pequenos`, dominio de mail,
      Instagram, Cloudinary preset — son datos/infra de la tienda, no del
      código

---

## 3. Inventario del modelo actual (CORE vs específico-de-ropa vs plataforma)

Hecho el 2026-09-15 leyendo las 26 entidades. Sirve de mapa para cuando se
retome la modularización real.

| Entidad | Categoría | Nota |
|---|---|---|
| Product *(salvo 2 campos)* | CORE | acoplamiento de talle, ver abajo |
| Order / OrderLine | CORE *(con 1 campo acoplado)* | `OrderLine.size` es obligatorio siempre |
| Discount, Coupon | CORE | `Discount.kind=PARAMETRO` ya usa ParamGroup genérico |
| ParamGroup / ParamOption / ProductParam | CORE, ya genérico | motor de atributos reusable — buen activo para módulos futuros |
| AdminUser, Role, Permission | CORE | `dni`/`email` únicos **globalmente** (a revisar si algún día hay multi-tenant) |
| HeroSlide, Supplier, Shift, CashRegister | CORE | sin acoplamiento a ropa |
| **SizeScale, SizeStock, `Product.sizeScaleId`** | **Específico de ropa, embebido en Product** | no es un módulo aparte, está soldado a la entidad Product |
| `Product.ageRange` | Específico de ropa (indumentaria infantil) | campo hardcodeado en Product |
| SiteSettings, MarketingConfig | Hoy singleton global | tendrían que pasar a "una fila por tenant" el día que haya multi-tenant |
| PlatformMailSettings | Correctamente platform-level | config del operador SaaS, no de cada tienda — **no tocar**, ya está bien pensada |

### Problemas concretos detectados (documentados, no resueltos — se resuelven cuando se retome multi-tenancy)

1. **Talle embebido en `Product`/`OrderLine`**, no como sistema de variantes
   genérico. Generalizarlo (talle → "variante" reusable por otros rubros) es
   trabajo real, no un simple move de archivos.
2. **3 singletons globales** (`SiteSettings`, `MarketingConfig`, y el ya
   correcto `PlatformMailSettings`) asumen una sola tienda por deploy.
3. **Constraints únicos globales** que deberían ser por tenant:
   `Coupon.code`, `AdminUser.dni`/`email`.

---

## 4. Fases

Convención de estado: ✅ Hecho · 🔜 Próximo paso (aplica ahora, mientras hay
un solo e-commerce) · ⏸️ Futuro — diferido hasta validar la tienda y decidir
ofrecerla a otros.

### Fase 0 — Análisis y este documento
- [x] Leer y opinar sobre `propuesta_ecommerce_saas.txt`
- [x] Analizar modelo actual (26 entidades) — inventario CORE/ropa/plataforma (sección 3)
- [x] Crear este documento de plan

### Fase 1 — Rename del proyecto ✅
- [x] Confirmar nombre (sección 2)
- [x] Ejecutar rename de paquete/pom.xml/docs (DB queda como estaba, ver sección 2)

### Fase 2 — Repackage por feature ✅

Hecho el 2026-09-15. **Decisión explícita con el usuario:** esto revierte
la organización package-by-layer (`model/`, `repository/`, `service/`,
`controller/`, `dto/`) que estaba documentada en `PROYECTO.md` como
"pedido del cliente" — se confirmó antes de ejecutar (ver AskUserQuestion
de esta sesión) porque era un cambio de fondo, no cosmético.

112 archivos movidos (con `git mv`, historial preservado) a package-by-feature:

```
core/product/    core/order/     core/discount/  core/coupon/    core/param/
core/admin/      core/hero/      core/supplier/  core/shift/     core/settings/
core/marketing/  core/exchange/  core/tenant/    core/auth/      core/dashboard/
core/export/     core/ (PageResponse, compartido)
modules/ropa/    -> SizeScale, SizeStock, integración con Product
platform/        -> PlatformMailSettings (config del operador, no de cada tienda)
```
`config/` y `common/` quedaron como estaban (son infraestructura
transversal, no un tema de negocio).

- [x] Mapeo completo de las 112 clases a su paquete nuevo
- [x] Move + fix de `package` declarations (script)
- [x] Fix de imports (3 pasadas: imports directos, imports de miembros
      anidados tipo `X.Dtos.Nested`, y referencias completamente
      calificadas inline en el código — las tres formas que usa este
      codebase para referenciar clases de otro paquete)
- [x] Fix manual de ~10 imports que quedaron implícitos (dos clases que
      antes compartían paquete por capa — ej. `Product`/`SizeStock` — y al
      separarse por feature necesitaban un import explícito que antes no
      hacía falta)
- [x] Carpetas viejas vacías (`model/`, `controller/`, `service/`,
      `repository/`, `dto/`) eliminadas
- [x] `PROYECTO.md` y `README.md` actualizados con la estructura nueva
- [x] Verificado: compila, compilan los tests, y los 28 tests pasan

**Nota técnica:** `core/product/Product.java` importa
`modules/ropa/SizeStock.java` — un core dependiendo de un módulo es lo
inverso de lo ideal (debería ser al revés), pero es el mismo acoplamiento
ya documentado en la sección 3 (#1) que se resuelve recién en la
generalización de talle→variante, no en este repackage.

### Fase 3 — Infraestructura de tenant (base, sin activar todavía) ✅
Hecho el 2026-09-15. Se agregó el mecanismo de tenant sin tocar ninguna
entidad de negocio ni cambiar comportamiento — 100% aditivo:

- [x] Entidad `Tenant` (`id`, `slug`, `name`, `active`, `createdAt`) —
      mínima a propósito, sin `domain`/`plan`/`theme` todavía (eso se suma
      cuando haga falta, Fase 5/6/8)
- [x] `TenantService.ensureDefault()`: crea la única fila de tenant al
      arrancar (desde `app.tenant.slug`/`app.tenant.name`, seedeable por
      env vars `TENANT_SLUG`/`TENANT_NAME` igual que `ADMIN_*`)
- [x] `TenantContext` (`common/`): holder del tenant de la request actual
- [x] `TenantResolutionFilter` (`config/`): resuelve el tenant en cada
      request y lo deja en `TenantContext`. Hoy resuelve siempre "el único
      tenant activo" — **no lee el header `Host` todavía**. Es el único
      lugar que va a cambiar cuando haya resolución real por dominio; el
      resto del código no necesita tocarse porque ya va a estar leyendo de
      `TenantContext`
- [x] Verificado: compila, los 28 tests pasan

**Importante — lo que esto NO hizo (a propósito):** ninguna entidad de
negocio (`Product`, `Order`, `AdminUser`, etc.) tiene todavía columna
`tenant_id`, y ningún repository/service filtra por tenant. `TenantContext`
existe pero nada lo lee todavía. Eso es la Fase 4, mucho más invasiva
(toca ~10 entidades y todos sus repositories/services + JWT), y antes de
hacerla hay una decisión de producto a confirmar (ver Fase 4).

### Fase 4 — `tenant_id` en entidades CORE + aislamiento real ✅

Hecho el 2026-09-15. Alcance completo: 13 entidades pasaron a ser
tenant-scoped, con filtrado explícito en cada repository/service (no
Hibernate `@Filter` automático — se prefirió explícito por auditabilidad,
ver la discusión que quedó más abajo).

**Entidades con `tenant_id` agregado:** Product, Order (+ Exchange, mismo
patrón de correlativo por tenant), Discount, Coupon, ParamGroup, SizeScale,
HeroSlide, Supplier, Shift, MarketingSend, AdminUser, Role.

**Convertidas de singleton global a "una fila por tenant":** SiteSettings y
MarketingConfig — el id de la fila ahora ES el id del tenant (relación 1:1,
sin columna extra). `PlatformMailSettings` se dejó como estaba (es config
del operador de la plataforma, no de cada tienda — correcto que siga
global).

**`AdminUser`/`Role` — el caso especial (confirmado con el usuario):** un
admin de tienda pertenece a un único tenant; el superadmin (operador de la
plataforma) tiene `tenantId = null` y no pertenece a ningún tenant.
`AdminUserRepository.findByDniForTenant(dni, tenantId)` resuelve un DNI
contra el tenant actual O contra el superadmin — es lo que usan login y
`JwtAuthFilter`. Mismo patrón en `Role` (el rol de sistema "Superadmin"
tiene `tenantId = null`, compartido).

**Constraints únicos que pasaron de globales a compuestos (tenant_id + X):**
`Coupon.code`, `Order.number`, `Exchange.number`, `AdminUser.dni`,
`AdminUser.email`, `Role.name`.

**Decisión técnica:** filtrado explícito (`findByIdAndTenantId`,
`findByTenantId...`) en vez de un filtro automático de Hibernate. Es más
código pero es imposible "olvidarse de activarlo" — alineado con la
sección 15 de `propuesta_ecommerce_saas.txt` ("no confiar únicamente").
Cada `get(id)` de cada service quedó como el único punto de entrada para
mutar una entidad, y todos filtran por tenant — eso previene IDOR
(que alguien de un tenant edite/borre algo de otro adivinando el id).

**Jobs en background (@Scheduled) no tienen tenant de request:**
`MarketingCampaignScheduler` no pasa por `TenantResolutionFilter` (no hay
HTTP request). Se resuelve explícitamente: itera los tenants activos y
setea `TenantContext` a mano antes de correr la campaña de cada uno — hoy
un solo tenant, pero el loop ya queda listo para varios.

**Verificado:** compila y los 28 tests pasan (tuve que arreglar un test que
llamaba directo a un método de repository que cambió de firma).

**Lo que esto NO hizo (a propósito, sigue diferido):**
- Resolución de tenant por Host/dominio — `TenantResolutionFilter` sigue
  resolviendo siempre "el único tenant activo", no lee el header `Host`.
- El JWT no lleva un claim de tenant — no hace falta mientras haya un solo
  tenant posible.
- Generalizar talle → variante genérica (sigue siendo Fase 3, diferida).

**Resuelto — pendiente operativo de la MySQL local:** como agregar
`tenant_id` (NOT NULL) rompía el arranque contra la base MySQL local
existente (`ddl-auto=update` no puede agregar una columna NOT NULL sin
default a una tabla con filas), y no había nada importante cargado
todavía, se decidió (con el usuario) aprovechar y **renombrar también la
base de `estilos_pequenos` a `saasweb`** — ya no hace falta distinguir
"identidad de código" de "identidad de datos" para el nombre de la DB en
este punto. `application.yml` y los scripts `database/*.sql` (que siguen
sin actualizar el ESQUEMA a la versión con `tenant_id` — sólo se les
cambió el nombre de la base) quedaron apuntando a `saasweb`. La base vieja
`estilos_pequenos` queda huérfana en MySQL (no se borró); se puede eliminar
a mano (`DROP DATABASE estilos_pequenos;`) cuando se confirme que no hace
falta. Al arrancar la app, `createDatabaseIfNotExist=true` crea `saasweb`
desde cero con el esquema nuevo.

### Fase 4 (histórico) — texto original antes de ejecutar, dejado como referencia
Agregar `tenant_id` a las entidades CORE, hacer que los repositories/services
filtren siempre por `TenantContext.getTenantId()`, convertir los 3
singletons (`SiteSettings`, `MarketingConfig`) en "una fila por tenant", y
arreglar los uniques globales que deberían ser por tenant (`Coupon.code`,
`AdminUser.dni`/`email`, `Role.name` — sección 3).

**Antes de arrancar esta fase hay que confirmar un supuesto de producto**
(se puede equivocar caro si se asume mal y hay que revertir):
`AdminUser`/`Role` — ¿un admin de tienda pertenece siempre a un único
tenant (nunca gestiona más de una tienda), y el `superAdmin` (operador de
la plataforma) queda fuera del esquema de tenant, tal como ya sugiere el
flag `AdminUser.superAdmin` que existe hoy? Este documento asume que sí
(es el diseño más simple y consistente con lo que ya existe) salvo que se
diga lo contrario antes de implementar.

También hay una decisión técnica (no de producto, se puede decidir en el
momento): filtrar por tenant con parámetros explícitos en cada query
(`findByTenantIdAndX`, más verboso pero visible/auditable — alineado con
la sección 15 de `propuesta_ecommerce_saas.txt`: "no confiar únicamente")
vs. un filtro automático de Hibernate (`@Filter`, menos código pero más
fácil de olvidar activar y generar una fuga entre tenants sin darse
cuenta). Recomendación: explícito.

Ver `propuesta_ecommerce_saas.txt` secciones 2-4 y 15-16 para más detalle.

### Fase 5 — Planes, límites y módulos activables por tenant ✅ (solo infraestructura)

Hecho el 2026-09-15. **Alcance confirmado con el usuario:** solo el
mecanismo, sin definir precios/planes/límites reales todavía (los números
de la propuesta original eran "solo un ejemplo").

- [x] Entidad `Plan` (`core/plan/`): `slug`, `name`, `maxProducts`
      (`Integer`, null = sin límite), `maxAdminUsers` (ídem),
      `enabledModules` (`Set<String>`, ej: `"ropa"`), `showPlatformBranding`
      — catálogo compartido (varios tenants podrían apuntar al mismo plan),
      no una fila por tenant
- [x] `Tenant.planId` (NOT NULL) — todo tenant tiene un plan asignado desde
      que se crea; `TenantService.ensureDefault()` crea el plan por defecto
      en la misma operación que crea el tenant (evita el problema de
      columna NOT NULL en una fila a medio crear)
- [x] `PlanService.ensureDefault()`: siembra UN plan permisivo (`maxProducts
      = null`, `maxAdminUsers = null`, `enabledModules = {"ropa"}`,
      `showPlatformBranding = false`) — cero impacto en la tienda actual
- [x] **Límites realmente enforced** (no solo el dato, la validación):
      `ProductService.create()`/`duplicate()` y `AdminUserService.create()`
      chequean el límite del plan del tenant antes de crear, tiran 400 con
      mensaje claro si se pasaría del límite
- [x] Verificado: compila y los 28 tests pasan (el plan por defecto sin
      límites no bloquea nada de lo que ya hacían los tests)

**Lo que esto NO hizo (a propósito):**
- **`enabledModules` no está gateado en ningún lado todavía.** El campo
  existe (el plan por defecto ya trae `"ropa"` habilitado) pero no hay
  ningún endpoint/funcionalidad que lo consulte para activar/desactivar
  algo — no tiene sentido gatear el único módulo que existe hoy y que es
  obligatorio para la tienda actual. Se activa el día que haya un segundo
  módulo real que se pueda deshabilitar.
- **Sin `PlanController`/endpoints de gestión de planes** — no hay todavía
  un catálogo de planes que administrar desde el panel; cargar planes
  reales (Básico/Profesional/etc.) el día que se definan es un INSERT/UPDATE
  directo en la tabla `plan`, no requiere código nuevo.
- **Sin test automatizado de la validación de límites** — la lógica se
  revisó a mano y no rompe nada existente (el plan sembrado no tiene
  límites), pero no hay un test de integración que fuerce el límite y
  verifique el 400. Pendiente si se quiere blindar antes de cargar límites
  reales.
- **`showPlatformBranding` no está conectado a nada del frontend** — el
  campo existe en el backend pero mostrar/ocultar el "Powered by..." es
  trabajo de Angular, fuera de este repo.

### Fase 6 — Themes en Angular 🔜 (mecanismo de marca probado + modo oscuro parcial)

Hecho el 2026-09-15, mismo criterio de "un paso chico" que la Fase 7: se
construyó y **verificó de verdad** el mecanismo de cambio de theme en
runtime, sin inventar una segunda paleta especulativa (no hay pedido de
diseño concreto para eso todavía).

**Backend**: `SiteSettings.theme` (`String`, nullable — null se trata como
`"default"`), expuesto en `GET /api/settings` (público). Sembrado
`"default"` para la tienda actual. No editable por API todavía (no hay
nada más entre qué elegir).

**Frontend**: `SettingsService` aplica `document.documentElement.setAttribute
('data-theme', theme)` al cargar/actualizar settings — mismo patrón que ya
usaba `applyFavicon`.

**Verificado en el navegador (no sólo leído en la doc de Tailwind):** con
la app corriendo de verdad, se confirmó `data-theme="default"` en
`<html>`, y se probó **en runtime** que sobreescribir la variable CSS
`--color-brand-500` (`document.documentElement.style.setProperty(...)`)
cambia instantáneamente el color de elementos que usan la utility
`bg-brand-500` (Tailwind v4 genera las utilities referenciando la
variable, no un valor fijo — confirmado, no asumido). Esto prueba que el
mecanismo real para ofrecer temas (una regla `[data-theme="x"] { ... }`
en `styles.css`) va a funcionar sin tocar ningún componente.

**Lo que esto NO hizo (a propósito, sigue pendiente):**
- No se creó ningún theme de MARCA nuevo (serían decisiones de diseño —
  colores, tipografías — que nadie pidió todavía).
- Sin selector de theme de marca en el admin (no hay entre qué elegir
  todavía). El modo oscuro (ver abajo) es otro eje, no esto.
- `styles.css` no tiene ninguna regla `[data-theme="x"]` todavía — el
  `@theme` actual sigue siendo el único/default de marca, sin cambios
  visuales por ese lado.

**Modo oscuro — segundo paso, alcance acotado (2026-09-15):** a pedido
del usuario, se implementó un modo oscuro real, pero **como eje aparte del
theme de marca**: es una preferencia de quien visita (se guarda en
`localStorage` del navegador, no en el backend), no una decisión del
tenant. Mecanismo: `ThemeModeService` (frontend) setea
`data-mode="dark"|"light"` en `<html>` — inicial: `localStorage` si ya
eligió antes, si no `prefers-color-scheme` del sistema. `styles.css` define
`@custom-variant dark (&:where([data-mode="dark"], [data-mode="dark"] *))`
para que las clases `dark:` de Tailwind v4 reaccionen a ese atributo (en
vez de sólo seguir el SO). Botón toggle (☀️/🌙) en el header.

**Alcance deliberadamente chico:** sólo el header y la sección de
logo/intro + el título de "Lo más vendido" de la home tienen clases
`dark:`. El resto del sitio (grilla de productos, catálogo con filtros,
ficha de producto, checkout, todo el panel de admin) **todavía no
reacciona al modo oscuro** — se ve igual en los dos modos. Sumar más
páginas es trabajo iterativo futuro, página por página.

**Verificado en el navegador:** con la app corriendo de verdad, se probó
el toggle en los dos sentidos (claro→oscuro→claro), se confirmó que
recolorea header + hero + título correctamente, y que `localStorage`
guarda la elección.

### Fase 7 — Personalizador visual (bloques de página) 🔜 (2 bloques + pantalla de admin)

Hecho el 2026-09-15, **un bloque a la vez** (el proyecto frontend tiene su
propio `CLAUDE.md` que pide cambios chicos e iterativos, no specs grandes
de una — se respetó eso en vez de implementar todo el personalizador de
una sola tanda).

**Backend** (`core/page/`): entidad `PageBlock` (`tenantId`, `pageType`
—hoy sólo `"HOME"`—, `blockType`, `position`, `visible`). Sin campo
`configuration` todavía (se agrega el día que un bloque realmente
necesite datos propios). `GET /api/page-blocks` público (sólo visibles,
en orden), `GET/PUT /api/admin/page-blocks/**` gateados por
`CAROUSEL_MANAGE` (mismo permiso que ya gatea el resto del carrusel —
reservado a superadmin, no a "Administrador", igual que hoy).
`PageBlockService.ensureDefaultHomeBlocks()` siembra dos bloques por
tenant: `HERO` (posición 0) y `FEATURED_PRODUCTS` (posición 1, envuelve la
sección "Lo más vendido" que ya existía).

**Frontend** (`frontend-ecommerce---ruth`, repo separado — ver su propio
`PROYECTO.md` §7 y §11 #52-54): `PageBlocksService` lee el endpoint
público (`isVisible(blockType)`) y el endpoint de admin
(`adminBlocks`/`setVisible`). `CatalogPageComponent` envuelve el hero y
"Lo más vendido" en sus respectivos `@if`. **Pantalla de admin nueva**
(`/admin/inicio`, componente `AdminPageBlocksComponent`, gateada por
`CAROUSEL_MANAGE`): lista los bloques de la home con un botón
Mostrar/Ocultar por bloque — reemplaza tener que llamar la API a mano.

**Verificado de punta a punta en el navegador** (no sólo tests): con
ambos servidores corriendo contra MySQL real — reseteada para que se
sembraran los dos bloques —, se entró como superadmin a `/admin/inicio`,
se ocultó "Lo más vendido" desde la pantalla nueva, se confirmó que
desaparecía de la home pública, y se volvió a mostrar. Backend: compila y
los 28 tests pasan.

**Lo que esto NO hizo (a propósito, sigue pendiente):**
- Sin reordenamiento real en la UI (el campo `position` existe, pero con
  2 bloques fijos no hay drag&drop todavía).
- Sin `configuration` por bloque (ningún bloque actual necesita datos
  propios).
- Sin más tipos de bloque (banner de texto, categorías, etc.) — se suman
  cuando haya un pedido concreto de cuál.
- Sin theming (eso es la Fase 6, todavía no arrancada).

### Fase 8 — Dominios propios, SSL, deployment con Docker + reverse proxy ⏸️

### Fase 9 — Agregar un segundo rubro reutilizando la plataforma ✅ (alta de tenant, selector modo demo y themes reales)

Rubros concretos definidos con el usuario: **ferretería** y **venta de
repuestos de vehículos** (2026-09-15).

**Hallazgo clave:** el usuario confirmó que estos rubros NO necesitan stock
desglosado por variante (a diferencia de talle en ropa — una remera tiene
stock distinto por talle; un tornillo o un filtro de aceite tiene un único
número de stock). Esto cambia radicalmente el alcance de la fase:

- **NO hace falta generalizar `SizeScale`/`SizeStock`** ni tocar `Product`,
  `OrderLine`, checkout, métricas, caja o exports — el acoplamiento a
  talle documentado en la sección 3 #1 **no bloquea** estos dos rubros.
- **Los atributos de ambos rubros encajan directo en `ParamGroup`/
  `ParamOption`**, que ya es 100% genérico (lo mismo que usa hoy "Tipo de
  prenda" o "Estación" para ropa). Agregar el rubro es prácticamente
  **cargar datos**, no escribir código.

**Bloqueador real:** hoy existe un solo tenant (la tienda de ropa real).
No tiene sentido cargar categorías de ferretería/repuestos en su catálogo.
Por eso se preparan las plantillas como referencia, listas para sembrar el
día que exista un segundo tenant — **no se cargan en ningún tenant ahora**.

#### Plantilla — Ferretería

| Grupo (`ParamGroup`) | `multiple` | Opciones de ejemplo |
|---|---|---|
| Marca | no | (abierto — alta libre por el dueño, no una lista fija) |
| Medida | no | según el producto (ej: 3mm, 4mm, 5mm, 1/2", 3/4"...) |
| Material | no | Acero, Acero inoxidable, Bronce, Plástico, Aluminio |
| Unidad de venta | no | Unidad, Metro, Kilogramo, Caja, Par |
| Características técnicas | sí (`multiple=true`) | Resistente a la corrosión, Uso exterior, Uso industrial... |

#### Plantilla — Repuestos de vehículos

| Grupo (`ParamGroup`) | `multiple` | Opciones de ejemplo |
|---|---|---|
| Marca del vehículo | no | Toyota, Ford, Volkswagen, Chevrolet, Fiat, Renault... |
| Modelo | no | (depende de la marca elegida — considerar UI dependiente cuando se implemente) |
| Año | sí (`multiple=true`, es un rango) | 2010, 2011, 2012... (o un rango desde/hasta si se prefiere modelar distinto) |
| Cilindrada | no | 1.4, 1.6, 1.8, 2.0, 2.4... |
| Compatibilidad | sí (`multiple=true`) | permite marcar que una pieza sirve para varios modelos/años a la vez |
| Código OEM | — | esto es un dato de texto libre por producto, no una parametría — candidato a campo directo en `Product` (o un `ProductParam` de texto libre si se prefiere no tocar el modelo) cuando se implemente de verdad |

**Nota de diseño para cuando se implemente:** "Compatibilidad" en repuestos
es conceptualmente distinto a "Marca" — un filtro de aceite puede servir
para 5 modelos/años distintos a la vez. El `ParamGroup.multiple=true` ya
soporta esto (un producto puede tener varias opciones del mismo grupo), así
que no hace falta modelo nuevo — mismo mecanismo que "Estación" en ropa,
donde un producto puede ser de varias estaciones a la vez.

**Qué falta para poder cargar esto de verdad (no ahora, cuando haya tenant real):**
1. Mecanismo para crear un segundo tenant (hoy no existe — es la Fase 8,
   deliberadamente diferida "hasta que despleguemos").
2. Decidir si el segundo tenant es un módulo *nuevo* dentro de la misma
   plataforma (share de `AdminUser`/`Role`/etc. patterns ya existen) o si
   arranca de cero — con la arquitectura actual (todo tenant-scoped desde
   la Fase 4), es simplemente: crear la fila `Tenant`, crear su `AdminUser`
   inicial, y sembrar estas `ParamGroup` en vez de las de ropa.

#### Implementación: alta de tenant + selector modo demo (2026-09-15)

Motivación real del usuario (no es sólo "agregar un rubro más"): tener un
asistente **"Crear tienda"** en el panel — elegir un rubro de una lista
extensible (ropa, ferretería, repuestos, más a futuro), crear la tienda, y
verla funcionando **en local**, para mostrarlo en su clase. Ver también la
sección 1 (`X-Demo-Tenant`) — el mecanismo que permite ver varias tiendas
locales sin subdominios/DNS reales.

**Backend (hecho y verificado contra MySQL real):**
- `Rubro` (enum: `ROPA`/`FERRETERIA`/`REPUESTOS`) — agregar un rubro nuevo
  a futuro es agregar un valor acá + su plantilla de seed, no reescribir
  nada. `Tenant.rubro` (NOT NULL) guarda el elegido; también define el
  theme por defecto (`Rubro.defaultTheme`, hoy sólo usado como valor de
  `SiteSettings.theme` — el CSS real de esos themes todavía NO existe, ver
  "Qué NO se hizo" abajo).
- `TenantService.create(name, slug, rubro)` — crea la fila `Tenant` (slug
  único, plan por defecto). `TenantService.findAll()` /
  `resolveIdBySlug(slug)` para el listado y el selector demo.
- `TenantProvisioningService` (nuevo, `core/tenant/`) — orquesta el alta
  completa: crea el tenant, `SiteSettingsService.createFor(...)` (fila de
  settings con el nombre elegido, SIN hardcodear la marca de Estilos
  Pequeños), `PageBlockService.ensureDefaultHomeBlocks(...)`, y siembra
  2 `ParamGroup` + 3-4 productos de ejemplo específicos del rubro (con
  imagen ilustrativa generada como SVG data-URI, sin depender de
  Cloudinary). IDs con `UUID.randomUUID()` — no colisionan con los IDs de
  slug fijo que usa el seed de la tienda piloto (`DataSeeder`).
  Ferretería/repuestos usan una escala de talle trivial de un solo valor
  ("Único") en vez de generalizar `SizeScale`/`SizeStock`, siguiendo la
  decisión ya tomada de "un solo stock por producto" para estos rubros.
- `TenantController` (`/api/admin/tenants`, gateado `hasAuthority('SUPERADMIN')`
  igual que `/api/admin/settings/cloudinary`): `GET` lista tenants, `POST`
  crea uno (delega en `TenantProvisioningService`), `GET /rubros` devuelve
  el enum con label para no hardcodearlo en el front.
- **Selector de tienda modo demo**: `TenantResolutionFilter` ahora, si
  `app.tenant.demo-switch-enabled=true` (default en dev, apagable con
  `TENANT_DEMO_SWITCH_ENABLED=false`), lee el header `X-Demo-Tenant: <slug>`
  y resuelve contra ESE tenant en vez del único activo. No es resolución
  real por dominio — es un atajo a propósito para mostrar varias tiendas
  locales sin DNS/subdominios (decisión confirmada con el usuario:
  "Selector modo demo alcanza").
- `Product.ageRange` (y su DTO/service) dejó de ser obligatorio — es un
  concepto específico de indumentaria infantil que ferretería/repuestos no
  tienen; forzarlo hubiera obligado a inventar un valor sin sentido.

**Verificado end-to-end** (MySQL real, reset + reseed): creados dos
tenants nuevos (`el-yunque` FERRETERIA, `el-ciguenal` REPUESTOS) vía
`POST /api/admin/tenants`; `GET /api/settings` y `GET /api/products` con
el header `X-Demo-Tenant` devuelven marca/catálogo aislados y correctos
para cada uno; sin header, la tienda piloto (Estilos Pequeños) sigue
devolviendo exactamente lo mismo que antes — cero regresión.

**Frontend (hecho y verificado, `frontend-ecommerce---ruth`):**
- `TenantAdminService` (listado/alta de tiendas + `GET /rubros`) y
  `DemoTenantService` (guarda el slug elegido en `sessionStorage`, por
  pestaña — no queda pegado entre sesiones ni afecta a otros visitantes).
- `demoTenantInterceptor` agrega `X-Demo-Tenant` a toda request `/api/*`
  mientras haya una tienda demo elegida — **incluye las requests del
  panel admin**, no sólo el sitio público: mientras el superadmin está
  "viendo" una tienda demo, todo `/admin/**` (productos, parametrías,
  etc.) opera sobre ESA tienda, no sobre la propia. Es el comportamiento
  esperado del mecanismo (mismo tenant para todo, ver
  `TenantResolutionFilter`), no un caso aparte a cubrir.
- Asistente **"Crear tienda"** en `/admin/superadmin/tiendas` (sólo
  superadmin, mismo criterio que Cloudinary/mail): formulario
  nombre + slug (autogenerado, editable) + rubro, listado de tiendas
  existentes con botón "Ver esta tienda", y banner "Estás viendo la
  tienda demo `<slug>`" con botón para volver a la tienda por defecto.
- Verificado de punta a punta en el navegador (`ng serve` + backend real):
  creada una tienda de ferretería desde el asistente, redirige sola al
  sitio público y la muestra; los filtros del catálogo ya muestran los
  `ParamGroup` propios del rubro ("Material", "Unidad de venta"); filtrar
  por "Acero" devuelve exactamente el único producto con esa opción
  (confirma que `ProductParam.optionId` quedó bien enlazado al id real
  de cada `ParamOption`, no a su label). Botón "Volver a la tienda por
  defecto" limpia el `sessionStorage` y restaura Estilos Pequeños sin
  ningún cambio visible.

**Themes reales `ferreteria`/`repuestos` (hecho, mismo día):** agregados
en `styles.css` reasignando los tokens de `@theme` (`--color-brand-*`,
`--color-mint-*`, `--font-display`, `--font-sans`) dentro de
`[data-theme="ferreteria"]` / `[data-theme="repuestos"]` — mismo
mecanismo verificado en Fase 6, sin tocar ningún componente. Paleta
adaptada (no copiada 1:1) del mockup standalone ("El Yunque y El
Cigüeñal"): ferretería en rust/ochre con tipografía Oswald; repuestos en
steel-blue/graphite con tipografía Teko; ambos sobre fondo claro (a
diferencia del mockup, que hacía "El Cigüeñal" oscuro por diseño) — el
`body` de la app tiene el color de texto hardcodeado, no tokenizado, así
que un theme oscuro real habría requerido auditar contraste en cada
componente, fuera de alcance de este paso. `--color-mint-*` (verde de
"ahorro/confirmar": envío gratis, botón de enviar pedido) se mantiene
verde en los tres themes — es semántico, no de marca. Verificado en el
navegador: header, hero, footer, filtros del catálogo y tarjetas de
producto de `El Yunque` y `El Cigüeñal` cambian de paleta/tipografía
correctamente; la tienda piloto (sin `data-theme` propio, usa
`"default"`) no tuvo ningún cambio visual.

**Logo, tagline y carrusel por tenant (hecho, mismo día — a pedido del
usuario tras notar que "El Yunque" se veía con el logo y el texto de
Estilos Pequeños):**
- La tagline hardcodeada de la home ("Indumentaria infantil con onda 🌈
  Elegí...") se acotó a la parte genérica ("Elegí, agregá al carrito y
  coordinamos la compra por WhatsApp.") — ya no asume ropa infantil, sin
  necesidad de un campo nuevo en `SiteSettings`.
- `RubroImages` (nueva clase, `core/tenant/`) genera imágenes de ejemplo
  como SVG data URI — no depende de subir nada a Cloudinary: `logo(emoji,
  color)` (logo circular), `productIcon(emoji, color)` (foto de
  producto) y `heroBanner(...)` (banner ancho 21:9 con degradé, texto y
  emojis decorativos). `Rubro` sumó `logoEmoji`/`logoColor` por rubro
  (🔧 rust para ferretería, ⚙️ steel-blue para repuestos, 👕 naranja para
  ropa).
- `TenantProvisioningService.provision()` ahora también le da a cada
  tienda nueva un logo propio (antes quedaba `null` → caía al logo real
  de Estilos Pequeños por el fallback estático del frontend) y 2 fotos de
  carrusel (`HeroSlide`) con copy de marketing propio del rubro — antes
  el carrusel quedaba vacío en toda tienda nueva.
- `DataSeeder.backfillHeroSlidesAndLogos()` (nuevo, corre en cada arranque
  del backend): completa carrusel/logo de tenants que ya existían de
  antes de este cambio — sin efecto si ya están completos (mismo patrón
  `ensureXxx` que el resto del seeder). La tienda piloto sólo recibe
  carrusel — su logo real lo sigue manejando el fallback estático del
  frontend (`LOGO_FALLBACK`), no este seeder.
- Verificado en el navegador: reiniciado el backend, backfill corrió solo
  y sembró carrusel para las 4 tiendas + logo para las 3 que no eran la
  piloto (visto en el log de arranque); "El Yunque" y "El Cigüeñal"
  muestran su propio logo, tagline genérica y un carrusel con 2 banners
  temáticos; la tienda piloto sumó su propio carrusel sin perder su logo
  real ni cambiar nada más.

**Qué NO se hizo todavía:**
- No hay forma de borrar/desactivar un tenant creado por error desde el
  asistente (sin endpoint de baja todavía) — quedan sólo como datos de
  prueba locales, sin impacto real.
- Las fotos de producto siguen siendo el mismo estilo simple (círculo de
  color + emoji) que ya tenía la tienda piloto — no son fotos reales, pero
  ya eran así antes de este cambio; no se rediseñaron.

### Fase 10 — Layout + color de marca configurables por tenant ✅ (completa para Ropa; Pasos 1-11)

A pedido del usuario: además del `theme` con nombre (Fase 6/9, sólo
colores/tipografía), poder elegir **un layout de página realmente
distinto** (estructura de home/catálogo) y **un color de marca libre**
(no una lista fija de paletas), como dos ejes independientes entre sí.
Confirmado con el usuario: el color se elige libre (color picker), no de
un catálogo curado; y el layout actual de la tienda piloto ("Estilos
Pequeños") queda como una opción más (`"classic"`), no se reemplaza.
Se avanza en 5 pasos chicos, cada uno verificado en el navegador antes
del siguiente (mismo criterio que las fases anteriores).

**Paso 1 (backend, ✅):** `SiteSettings.layout` (String, nullable) y
`SiteSettings.brandColor` (String, nullable, hex) — mismo patrón aditivo
que `theme`. `Rubro.defaultLayout` por constante (hoy `"classic"` para
los 3 rubros — es el único layout que existe). Verificado: `GET
/api/settings` de la tienda piloto devuelve `layout:"classic"` y
`brandColor:null` sin ningún otro cambio.

**Paso 2 (frontend, ✅):** `SettingsService` aplica `data-layout` en
`<html>` (mismo patrón que `data-theme`). Nuevo
`core/utils/color-ramp.ts` (`generateBrandRamp`): deriva las 8 paradas
`--color-brand-50..700` de un solo hex por mezcla con blanco/negro (el
500 queda igual al color elegido). Se aplican inline con
`style.setProperty` sobre `<html>`, por encima del `[data-theme]` con
nombre — mismo mecanismo de override en runtime verificado en Fase 6.
Verificado en el navegador: sin `brandColor`, cero cambio; fijando uno a
mano repinta header/hero/botones al instante.

**Paso 3 (frontend, ✅):** primer segundo layout real, `"editorial"` —
en vez de un componente Angular separado (duplicaría los ~150 líneas de
lógica de filtros/orden/paginación de `CatalogPageComponent`), se
implementó como una segunda rama de template dentro del mismo
`catalog-page.component.html` (`@if (layout() === 'editorial')`),
seleccionada por el signal `layout` del mismo componente — misma lógica
de catálogo para cualquier layout, sólo cambia la portada (hero de fondo
sólido, tipografía grande, sin avatar circular) y la grilla (3 columnas
en vez de 4). 100% basado en clases `brand-*`/`font-display` (nada de
color hardcodeado), para que cualquier `brandColor` lo pinte bien.
Verificado en el navegador contra `el-yunque` (seteando `layout`/
`brandColor` a mano en la fila de `site_settings` y revirtiendo después):
la tienda piloto no cambió ni un píxel; `el-yunque` con
`layout="editorial"` + un `brandColor` de prueba mostró la estructura
nueva repintada en ese color, mientras sus fotos de carrusel (imágenes,
no tokens) siguieron con su propia paleta — confirma los dos ejes
(layout, color) funcionando de forma independiente.

**Replanteo tras el Paso 3 — el gap real no era el formulario de "Crear
tienda":** al revisarlo con el usuario, un tenant nuevo no tiene ningún
`AdminUser` propio (`TenantProvisioningService.provision()` no crea
uno), así que no hay forma de loguearse como esa tienda para entrar a
`/admin/config`. Investigando el mecanismo de "modo demo"
(`X-Demo-Tenant` + `JwtAuthFilter.findByDniForTenant` con `tenantId IS
NULL` para superadmin + roles `system=true`) se confirmó que **ya
alcanza para que el superadmin edite cualquier tenant sin loguearse como
su admin** — no hizo falta backend nuevo para eso, sólo conectar la
navegación.

**Paso 4 (backend, ✅):** `PUT /api/admin/settings/appearance
{layout, brandColor}` (`SiteSettingsController`/`Service`/`Dtos`), mismo
nivel de permiso que `platform`. Validado con `curl` + `X-Demo-Tenant`.

**Paso 5 (✅):** pantalla "Apariencia" en `/admin/config/apariencia`
(`admin-appearance`, tarjeta nueva en el hub). Las miniaturas de diseño
son el `CatalogPageComponent` real escalado (nuevo `input()`
`layoutOverride`, para forzar un layout puntual sin pisar
`SiteSettings`) — no imágenes estáticas. El color se previsualiza sólo
dentro de las miniaturas (variables CSS scoped con `[ngStyle]`, reusando
`generateBrandRamp`), sin repintar el resto del panel hasta guardar.

**Paso 6 (✅):** botón "Configurar esta tienda" en
`admin-superadmin-tiendas` (junto a "Ver esta tienda"): activa el mismo
demo-switch y navega a `/admin/config` en vez de `/`. Con esto el
superadmin ya edita identidad, redes, WhatsApp, logo, carrusel y
apariencia de cualquier tienda — pantallas que ya existían, cero
duplicación. `submit()` ahora lleva ahí directo después de crear.
Verificado en el navegador contra `el-yunque` (cambio de nombre y de
apariencia, revertidos después, confirmados por SQL): sólo cambió esa
fila, el piloto y las demás tiendas quedaron intactas.

**Paso 7 (✅, 2026-09-16): consolidación final de layouts (6 en total) +
logo en cada uno.**

Los primeros 3 layouts nuevos que se probaron después del Paso 3
(`"editorial"`, `"marketplace"`, `"vidriera"`) se armaron de memoria vaga
de unos Figma que había mostrado el usuario antes en la conversación —
**el usuario los rechazó explícitamente dos veces** ("estas inventando
disenos que no tiene absolutamenta nada que ver con lo que te mostre";
"son iguales a la de estilo pequeno con cambios taan chicos que ni se
notan"). Se **borraron los 3** y se cambió de método: en vez de reconstruir
de memoria o clonar screenshots de sitios comerciales reales (se probó
también eso — el usuario pidió explícitamente ser honesto si no se podía
replicar exacto, y la respuesta fue que no, por la dependencia de
fotografía real del sitio), se usaron **6 temas de WordPress reales,
gratuitos/GPL, bajados por el usuario a una carpeta `templates/` en el
Escritorio** como referencia estructural (layout/composición, no código):
`shopper-store`, `rife-free`, `shoppingcart`, `big-store`, `botiga`,
`online-shop`. Se verificó la licencia (header GPL v2/v3 en `style.css` de
cada zip) antes de usarlos como referencia.

**Set final de 6 layouts** (todos como ramas `@if/@else if` dentro del
mismo `catalog-page.component.html`, no componentes separados — mismo
criterio que el Paso 3, para no duplicar la lógica de filtros/orden/
paginación):
- `classic` — el diseño original de la tienda piloto (avatar circular
  grande + degradé).
- `minimal` (inspirado en Botiga) — hero partido texto/foto sobre fondo
  gris.
- `boutique` (inspirado en Minna, visto en Framer) — foto grande a sangre
  con etiqueta de promo superpuesta.
- `curva` (inspirado en Rife Free) — foto oscurecida con borde inferior
  curvo, texto centrado.
- `grid` (inspirado en Shopper) — franja de foto chica + título, directo a
  la grilla.
- `mercado` (consolida Shopping Cart + Big Store + Online Shop — los 3
  eran estructuralmente casi idénticos: sidebar de categorías + banner +
  tarjetas de promo) — el más denso, tipo marketplace.

Fotos de las 5 demos nuevas: Unsplash, licencia gratuita de uso comercial,
buscadas con browser automation filtrando resultados patrocinados/iStock.
Íconos de producto de la demo "Ropa" (antes emoji+color) también se
cambiaron a fotos reales de Unsplash (`RubroImages.productIcon` sumó un
overload con `imageUrl`) — sólo para la demo de Ropa; production
tenants siguen subiendo sus propias fotos.

**Logo en cada layout (a pedido del usuario, tras notar que sólo Clásico
tenía uno grande):** se creó `shared/components/logo/logo.component.ts`
(`LogoComponent`, nuevo) — `<app-logo [src] [alt] [size] [extraClass]>`,
lee `SettingsService.settings().logoShape` (ver Paso 8) para decidir las
clases de recorte, `host: { style: 'display: contents' }` para no romper
el `flex`/`gap` de los contenedores que lo usan (bug real encontrado y
corregido: sin esto, el host del componente ocupaba su propio espacio de
layout y desalineaba el logo respecto al texto al lado). Reemplazó los
`<img>` sueltos con `CldImagePipe` en `header`, `footer`, y el avatar de
Clásico. Se agregó un `<app-logo>` en cada uno de los otros 5 layouts, en
el lugar que mejor encaja con la composición de cada uno (no el mismo
tratamiento en todos):
- `minimal`: chico, arriba del título, como marca de encabezado.
- `boutique`: al lado del nombre en la barra de texto debajo de la foto.
- `curva`: centrado, con anillo blanco, arriba del título (sobre la foto
  oscurecida).
- `grid`: al lado del nombre en la barra de título.
- `mercado`: al lado del nombre, dentro del banner (junto al texto "Envío
  gratis").

El header/footer YA mostraban el logo chico en los 6 layouts (es un
componente global, no depende del layout) — este paso agrega un SEGUNDO
lugar más protagónico específico de cada diseño, no reemplaza al del
header.

**Paso 8 (✅, 2026-09-16): forma del logo (`logoShape`).**

El logo se mostraba siempre recortado en círculo (`rounded-full`), sin
importar la forma real del archivo subido — un logo rectangular/cuadrado
quedaba mal. En vez de detectar automáticamente la forma del archivo (se
evaluó y se descartó: heurísticas de aspect-ratio son poco confiables y
generan sorpresas), se decidió con el usuario que sea una **elección
explícita** de quien sube el logo.

- Backend: `SiteSettings.logoShape` (String nullable — null se trata como
  `"circle"` en `SettingsResponse`, así ningún logo ya cargado cambia de
  golpe), validado `^$|^(circle|square|rectangle)$`. Sumado a
  `PlatformSettingsRequest` (edición de una tienda existente, vía
  `PUT /api/admin/settings/platform`) y a `TenantCreateRequest`/
  `SiteSettingsService.OnboardingExtras` (alta de tienda nueva vía el
  asistente — ver Paso 11). **No está en `AppearanceRequest`** (ese
  endpoint es sólo `layout`+`brandColor`+los 4 colores del Paso 9).
- Frontend: `LogoComponent.classes()` (computed) decide
  `rounded-full object-cover` (circle) / `rounded-lg object-cover`
  (square) / `rounded-md object-contain` (rectangle, con `max-width`
  mayor para no recortar el ancho). Selector de 3 botones (Redondo/
  Cuadrado/Rectangular) agregado en `admin-config-section` (pantalla
  "Identidad y contacto", junto al upload de logo existente) y en el
  asistente "Crear tienda" (paso Logo — ver Paso 11). El mini-mockup de
  `site-preview.component` (usado en esa misma pantalla, al costado del
  form) también refleja la forma elegida en vivo (`logoShapeClass`
  computed), aunque **NO refleja los colores granulares del Paso 9** —
  sólo se extendió para la forma del logo.

**Paso 9 (✅, 2026-09-16): colores independientes del color de marca.**

El usuario pidió, dos veces, poder elegir por separado el color del
encabezado, el del pie de página, el de los títulos/nombre y el del fondo
de la página — no sólo un color de marca único que deriva una rampa.

- Backend: `SiteSettings` sumó 4 campos nullable, todos hex, todos
  independientes entre sí y de `brandColor`: `headerColor`, `footerColor`,
  `textColor`, `pageBackgroundColor`. `null` en cualquiera = seguir
  derivando esa parte de `brandColor`/del layout, como siempre (cero
  cambio para tenants existentes). Sumados a `AppearanceRequest` (tienda
  existente) Y a `TenantCreateRequest`/`OnboardingExtras` (tienda nueva).
  **Nota:** `SiteSettingsService.applyOnboardingExtras` tenía ya 7
  parámetros String sueltos antes de este paso; en vez de seguir sumando
  parámetros posicionales (con 4 más iba a tener 11), se refactorizó a
  recibir un record `SiteSettingsService.OnboardingExtras` — si se agrega
  un campo más a futuro, va ahí, no como parámetro nuevo del método.
- Mecanismo (frontend) — **importante para no reinventar esto mal**: se
  evaluó y se DESCARTÓ hacer esto con variables CSS + `var(--x, revert)`
  (la idea era que el valor por defecto "revierta" a la clase Tailwind de
  siempre) — **no es válido**: la spec de CSS Custom Properties prohíbe un
  keyword de "CSS-wide" (`revert`/`initial`/`unset`) como fallback de
  `var()`, así que esa declaración quedaría inválida en todos los
  navegadores. El mecanismo real que se usó:
  - Header/Footer (`header.component`, `footer.component`): un
    `computed()` que lee `SettingsService.settings().headerColor`/
    `footerColor`/`textColor` directo, atado con
    `[style.background-color]`/`[style.color]`. Cuando el valor es
    `null`, Angular **quita** el estilo inline por completo (no lo pone en
    blanco) — así la clase Tailwind de siempre (incluida su variante
    `dark:`) sigue mandando. Esto NO sirve para preview-antes-de-guardar
    porque header/footer siempre leen el `SettingsService` global, nunca
    un borrador.
  - `CatalogPageComponent`: se agregaron 2 inputs nuevos,
    `textColorOverride`/`pageBgOverride` (mismo patrón ya existente de
    `layoutOverride`/`logoOverride`): `undefined` = usar `SiteSettings` de
    siempre, string/`null` = lo que mande un borrador (Apariencia o el
    asistente) sin tocar la config real. `pageBg` se aplica en el HOST del
    componente (`host: { '[style.background-color]': 'pageBg()' }`) — se
    ve en las zonas de cada layout que hoy NO tienen fondo propio (la
    sección "Lo más vendido" y la grilla del catálogo en los 6 layouts,
    verificado que ninguno declara un `background` ahí). `headingColor` se
    ató con `[style.color]` en el `<h1>` principal de cada uno de los 6
    layouts.
  - **A propósito NO se tocó**: el interior de las fotos/degradés de cada
    hero (`curva`, `boutique`, `mercado` tienen imagen de fondo — pisar
    sólo `background-color` ahí no cambia nada visualmente porque
    `background-image` se pinta encima), ni los mil matices de gris de
    texto secundario/párrafos/labels de cada layout — sólo el título/nombre
    de marca. Esto es una decisión de alcance, no un olvido: cubrir "cada
    gris posible" con un solo color rompería la jerarquía visual que cada
    layout ya tiene.
- UI: 4 selectores de color (Encabezado / Pie de página / Títulos y nombre
  / Fondo de la página, cada uno "elegido + quitar", mismo patrón que el
  picker de `brandColor` que ya existía) agregados en DOS lugares
  distintos, con implementación duplicada (NO es un componente
  compartido — si se quiere refactorizar a uno compartido, hoy hay 2
  copias de la misma UI a mantener):
  - `admin-appearance.component` (pantalla "Apariencia" de una tienda
    existente) — signals `draftHeaderColor`/`draftFooterColor`/
    `draftTextColor`/`draftPageBgColor`.
  - `tenant-wizard.component` (paso "Color" del asistente — ver Paso 11)
    — mismos 4 signals, mismo patrón, código separado.
  - Las miniaturas de diseño de `admin-appearance` y la vista previa del
    asistente pasan `[textColorOverride]`/`[pageBgOverride]` con el
    borrador actual a `<app-catalog-page>` para que el cambio se vea al
    instante sin guardar nada.
- **NO hecho**: el resumen de "Confirmar" del asistente (paso 6) NO
  muestra los 4 colores granulares elegidos (sólo muestra `brandColor` +
  logo + forma) — si se elige un color de encabezado/pie/etc. en el paso
  Color, no aparece en el resumen antes de crear. Gap de UX menor, no
  bloqueante.

**Paso 10 (✅, 2026-09-16): sugerir el color de marca a partir del logo.**

A pedido del usuario ("se podria hacer algo asi como elegir colores
acordes al logo y que el logo se analice"): nuevo
`core/utils/logo-color.ts` (`extractLogoColor(imageUrl): Promise<string |
null>`) — 100% client-side, sin pegarle al backend. Dibuja la imagen en un
`<canvas>` de 48×48, descarta píxeles casi blancos/negros/de baja
saturación (asume que son fondo del logo, no el color de marca), agrupa
los píxeles restantes en buckets de a pasos de 24 (para tolerar
antialiasing/compresión JPG) y devuelve el color promedio del bucket más
grande, en hex.

- Botón "Sugerir color de marca según el logo" en `admin-appearance`
  (sólo visible si la tienda ya tiene un logo guardado — usa
  `settingsService.settings().logoUrl`) y en el paso Color del asistente
  (sólo visible si ya se subió un logo en el paso anterior — usa el
  `logoDataUrl` en memoria del asistente, ver Paso 11; funciona porque una
  data URL nunca tiene problema de CORS con `canvas.getImageData`, a
  diferencia de una URL de Cloudinary cross-origin, que si algún día falla
  ahí es por eso — el código ya maneja el error con `try/catch` y
  `resolve(null)`, mostrando "no se pudo sacar un color claro, elegilo a
  mano").
- El color sugerido sólo pisa `draftBrandColor` — el usuario puede seguir
  ajustándolo a mano después (input de color normal), y los 4 colores
  granulares del Paso 9 son 100% independientes de esto.
- Este pedido fue lo que motivó reordenar el asistente para que el paso
  Logo vaya ANTES que el paso Color (ver Paso 11) — si no, no habría logo
  todavía para sugerir nada.

**Paso 11 (✅, 2026-09-16): asistente "Crear tienda" — reordenado a 7
pasos + logo diferido hasta confirmar.**

Orden final: **1 Diseño → 2 Logo → 3 Color (con sugerencia del logo) → 4
Identidad (WhatsApp/Instagram/Facebook) → 5 Previsualización (pantalla
completa) → 6 Confirmar → 7 Listo.** Antes eran 6 pasos (Diseño → Color →
Identidad-con-logo-adentro → Preview → Confirmar → Listo); el logo se
separó de "Identidad" a su propio paso y se movió antes de "Color" (ver
Paso 10, motivo).

Dos bugs reales encontrados por el usuario probando el asistente y
corregidos en el momento:
1. **El logo elegido no se veía en la vista previa** (ni en el panel
   persistente de los pasos 1-4, ni en la pantalla completa del paso 5).
   Causa: `CatalogPageComponent` leía el logo del `SettingsService` GLOBAL
   (la tienda piloto u otra ya activa), no el borrador local del
   asistente — la tienda todavía no existe en ese punto, así que no hay
   ningún `SiteSettings` propio para leer. Arreglado con un input nuevo,
   `logoOverride` (mismo patrón de override que `layoutOverride`/
   `textColorOverride`/`pageBgOverride`), pasado como
   `[logoOverride]="logoDataUrl()"` en las 2 instancias de
   `<app-catalog-page>` del asistente.
2. **El paso de Previsualización (5) no tenía botón "← Atrás"** — sólo
   "Ver a pantalla completa" y "Saltear este paso" (ambos hacia adelante).
   Se agregó "← Atrás" al lado de "Saltear este paso".

**Cambio de comportamiento importante, a pedido explícito del usuario**
("el logo... no se deberia subir a la nube si no crea la pagina"): el
archivo del logo YA NO se sube a Cloudinary al elegirlo en el asistente.
Se redimensiona localmente (reusa `resizeImageFile`, ya existía) y queda
SÓLO como una data URL en memoria (`logoDataUrl` signal) — esa misma data
URL sirve de preview (`<img [src]>`) Y de fuente para la sugerencia de
color (Paso 10). La subida real a Cloudinary ocurre recién dentro de
`crearTienda()`, justo antes de llamar a `tenantAdmin.create(...)` — si el
asistente se cancela antes de ese punto, nunca se mandó nada a Cloudinary
(cero imágenes húerfanas). **Este mismo problema (subida inmediata al
elegir el archivo) sigue existiendo tal cual en `admin-config-section`**
(pantalla "Identidad y contacto" de una tienda YA CREADA) — ahí SÍ tiene
sentido subir de inmediato porque la tienda ya existe y no hay "cancelar
la creación"; no se tocó y no hace falta tocarlo. El usuario también
mencionó "las fotos de carrusel" en el mismo pedido, pero **el asistente
hoy no tiene ningún paso de carrusel** (el carrusel se carga después de
creada la tienda, vía `/admin/carrusel`, donde la tienda ya existe de
verdad) — no había nada que diferir ahí; si en el futuro se agrega un
paso de carrusel al asistente, aplicar el mismo patrón de "diferir hasta
confirmar".

`TenantAdminService.TenantCreateRequest` (interfaz frontend) y el DTO
backend `TenantCreateRequest` quedaron con estos campos opcionales,
todos ignorados si vienen vacíos/ausentes: `layout`, `brandColor`,
`headerColor`, `footerColor`, `textColor`, `pageBackgroundColor`,
`whatsappNumber`, `instagram`, `facebookUrl`, `logoUrl`, `logoShape`.

**Verificado en el navegador (todos los pasos 7-11), sin tocar datos
reales:** los 6 layouts muestran su logo en el lugar esperado (miniaturas
de "Apariencia"); cambiar la forma del logo en "Identidad y contacto"
repinta el preview en vivo sin guardar; en el asistente, subir un logo de
prueba (PNG azul sólido generado por canvas) lo mostró correcto en las 2
vistas previas, "Sugerir color según el logo" devolvió exactamente
`#1d4ed8` (el color exacto del PNG de prueba) y repintó toda la vista
previa; los 4 colores granulares del asistente cambiaron el fondo/textos
de la vista previa en vivo; se confirmó con Network/consola que NO hay
ningún request a Cloudinary hasta tocar "Crear tienda", y que cancelar el
asistente después de subir un logo no genera ningún request tampoco.

**Discutido con el usuario pero explícitamente diferido — NO implementar
sin que el usuario lo pida de nuevo (no son bugs ni "olvidos"):**
- **Campos de formulario distintos según el layout elegido.** El usuario
  preguntó si algún día cada layout va a necesitar campos propios en el
  formulario de identidad (ej. tagline editable, foto de hero propia por
  layout). Respuesta dada en su momento: no todavía — hoy los 6 layouts
  comparten exactamente los mismos campos (`storeName`, `logoUrl`,
  `logoShape`, WhatsApp, Instagram, Facebook); esto recién se justifica el
  día que un layout concreto necesite un dato que otro no necesita.
- **Guía de tamaño de foto de hero por layout.** Se confirmó con el
  usuario que las fotos de hero de los 6 layouts SÍ tienen proporciones
  reales distintas entre sí (alturas fijas en `catalog-page.component.html`:
  `minimal` 280-380px, `boutique` 320-440px, `curva` 420-520px, `grid`
  224-288px, `mercado` variable con `aspect-ratio`, `classic` usa el
  carrusel de siempre a 21:9) — pero no se construyó ninguna guía/ayuda en
  el formulario de subida que le diga a quien sube una foto "para este
  layout conviene tal proporción". Sigue pendiente si se quiere agregar.
- **Selector de forma del logo por detección automática del archivo.** Se
  evaluó (¿se puede saber si el archivo subido es redondo/cuadrado/
  rectangular?) y se descartó a favor de la elección explícita del Paso 8
  — no hay heurística de detección automática en ningún lado del código,
  no intentar agregarla sin discutirlo de nuevo.

---

### Fase 11 — Ciclo de vida del tenant: pausar y eliminar ✅ (2026-09-16)

A pedido del usuario: un botón para pausar una tienda (dejarla de mostrar
al público sin borrar nada) y otro para eliminarla por completo (borrado
real e irreversible de todos sus datos). Dos features independientes,
gateadas igual que el resto de `TenantController` (`hasAuthority
('SUPERADMIN')`).

**Pausar / reanudar:**

- `Tenant.active` **ya existía** en el modelo desde la Fase 3 (default
  `true`) pero no estaba enforced en ningún lado salvo
  `MarketingCampaignScheduler` (que ya salteaba campañas de tenants
  inactivos) — no bloqueaba ver el storefront de una tienda "inactiva".
  Este paso lo conecta de verdad por primera vez.
- Nuevo: `TenantRepository.existsByIdAndActiveTrue`,
  `TenantService.isActive(tenantId)` / `setActive(tenantId, active)`.
- `TenantController`: `PATCH /api/admin/tenants/{id}/active
  {active: boolean}`.
- **Enforcement real, en `TenantResolutionFilter`** (después de resolver
  el tenant de la request): si el tenant resuelto NO está activo Y el
  path de la request no empieza con `/api/admin/` ni `/api/auth/` →
  responde `503` con JSON `{"error":"tienda_pausada","message":"Esta
  tienda está pausada."}` (con `setCharacterEncoding("UTF-8")` explícito —
  sin eso la tilde de "está" se mostraba mal, se detectó y corrigió en la
  misma verificación) y CORTA la cadena de filtros ahí (no llega ni a
  Spring Security ni al controller). La distinción es por **path**, no por
  si la request trae JWT — así el panel de administración de esa misma
  tienda (y el login, `/api/auth/**`) siguen accesibles siempre, pausada o
  no: es la única forma de que el superadmin (o el admin de esa tienda)
  pueda entrar a reanudarla.
- Frontend: `TenantAdminService.setActive(id, active, onSuccess, onError)`,
  botón "Pausar"/"Reanudar" + badge "Pausada" (con la fila atenuada,
  `opacity-60`) en `admin-superadmin-tiendas`.
- **LIMITACIÓN CONOCIDA, no construida:** la respuesta 503 es JSON plano
  para el backend — no hay ninguna pantalla "Esta tienda está pausada"
  en el frontend Angular. Qué ve exactamente un visitante real (la SPA
  falla a cargar `/api/settings` y de ahí en más no está definido/
  probado) no se verificó ni se construyó un manejo de error especial.
  Si se quiere una experiencia prolija para el visitante, falta esa
  pantalla — es candidato a próximo paso si se sigue esta fase.
- Verificado con `fetch` autenticado desde la consola del navegador
  (token real del `localStorage` de una sesión ya logueada): pausada
  `estilos-pequenos` vía `X-Demo-Tenant`, `GET /api/settings` público dio
  503 con el mensaje esperado; `GET /api/admin/tenants` con el MISMO
  header siguió dando 200; reanudada al toque después — cero impacto
  final en la tienda piloto.

**Eliminar (borrado permanente e irreversible):**

- Mecanismo de confirmación **elegido explícitamente por el usuario**
  (se le preguntó con `AskUserQuestion` dado el riesgo): "escribir el
  identificador exacto de la tienda" (mismo patrón que usa GitHub para
  borrar un repo), no un simple diálogo "¿Estás seguro?".
- Nuevo `core/tenant/TenantDeletionService.java` — recorre y vacía, EN
  ESTE ORDEN (documentado con comentarios en el archivo, no improvisar un
  orden distinto si se toca esto — rompe FKs):
  1. `Order` (cascada a `OrderLine` — `cascade=ALL, orphanRemoval=true` ya
     existía en la entidad; el borrado usa `deleteAllByTenantId`
     **derivado de Spring Data**, que borra entidad-por-entidad —
     NO un bulk `DELETE` SQL — a propósito, para que Hibernate respete esa
     cascada. Un bulk delete la hubiera saltado.)
  2. `Exchange` (cascada a `ExchangeLine`, mismo mecanismo)
  3. `ParamGroup` (cascada a `ParamOption` — **ojo:** el repository se
     llama `ParamRepository`, no `ParamGroupRepository`)
  4. `Product` (sus `@ElementCollection` — imágenes, tags, stock por
     talle — las borra Hibernate solo al borrar el `Product`, no
     necesitan tratamiento aparte)
  5. `Coupon`, `Discount`, `HeroSlide`, `MarketingSend`, `PageBlock`,
     `Shift`, `Supplier`, `SizeScale` (`modules/ropa/`) — sin relaciones
     entrantes entre ellos, el orden entre estos 8 no importa
  6. `AdminUser` — **tiene que ir antes que `Role`**: `AdminUser.role` es
     `@ManyToOne` SIN cascade; borrar el `Role` primero rompería esa FK
  7. `Role` (el filtro `tenantId = :id` ya excluye solo el rol de sistema,
     que tiene `tenantId = null` — nunca hace falta chequearlo aparte)
  8. `SiteSettings` (fila por id = tenantId, `deleteById` sólo si
     `existsById`)
  9. `MarketingConfig` (ídem, fila por id = tenantId)
  10. `Tenant` (al final)
  - Todo dentro de una única `@Transactional` — si algo falla a mitad de
    camino, se revierte todo.
  - Se agregó el método derivado `deleteAllByTenantId(String tenantId)` a
    estos 14 repositories (todos con un comentario `Ver
    TenantDeletionService`): `OrderRepository`, `ExchangeRepository`,
    `ParamRepository`, `ProductRepository`, `CouponRepository`,
    `DiscountRepository`, `HeroSlideRepository`,
    `MarketingSendRepository`, `PageBlockRepository`, `ShiftRepository`,
    `SupplierRepository`, `SizeScaleRepository`, `AdminUserRepository`,
    `RoleRepository`.
- `TenantController`: `POST /api/admin/tenants/{id}/delete
  {confirmSlug: string}` — el backend **vuelve a validar** que
  `confirmSlug` matchee el slug real de la tienda antes de borrar nada
  (no confía en que el frontend ya lo haya validado — mismo criterio de
  "no confiar únicamente" que ya se usó en la Fase 4 para el filtrado por
  tenant).
- Frontend: `TenantAdminService.deleteTenant(id, confirmSlug, onSuccess,
  onError)`, modal de confirmación en `admin-superadmin-tiendas` (input de
  texto + botón "Eliminar para siempre" deshabilitado hasta que el texto
  matchee el slug exacto, con anillo rojo mientras no matchea).
- **LIMITACIÓN CONOCIDA E IMPORTANTE — Cloudinary NO se limpia.** El
  preset de Cloudinary de esta plataforma es "unsigned" (sólo `cloudName`
  + `uploadPreset`, sin API key/secret guardada en ningún lado) — borrar
  assets requiere la Admin API firmada de Cloudinary, que esta app no
  tiene configurada. Al eliminar una tienda, sus fotos (logo, carrusel,
  productos) **quedan huérfanas en la cuenta de Cloudinary** — no hay
  forma de limpiarlas desde el código actual; hay que borrarlas a mano
  desde el dashboard de Cloudinary si se quiere liberar espacio. El
  pedido original del usuario fue "eliminar todos los datos... de la
  nube... de la db... todo" — **la parte de DB está 100% cubierta, la de
  "la nube" (Cloudinary) NO** por esta limitación de infraestructura. Si
  en algún momento se carga una API key/secret de Cloudinary real (dónde
  guardarla con seguridad es una decisión aparte, no trivial), ahí sí
  conviene volver a `TenantDeletionService` y agregar el borrado de la
  carpeta `{slug}/` completa en Cloudinary ANTES del borrado de DB (si
  Cloudinary falla, mejor no haber borrado nada todavía).
- Verificado de punta a punta en el navegador: creado un tenant
  descartable (`delete-test`) vía API directa, borrado desde la UI real
  (probado que un slug incorrecto deja el botón deshabilitado con anillo
  rojo, y que el slug correcto lo habilita y borra), confirmado que
  desaparece del listado de tiendas y que el resto (piloto incluida) no
  se tocó.

**NO hecho en esta fase (ninguno pedido explícitamente, anotado por si se
retoma):**
- Sin test automatizado (unitario ni de integración) para
  `TenantDeletionService`, el enforcement de pausa, ni ninguna feature de
  la Fase 10 ampliada — todo se verificó a mano en el navegador/consola
  esta sesión.
- Sin soft-delete / papelera de reciclaje — el borrado es directo y
  permanente, no hay forma de "deshacer" ni de recuperar una tienda
  borrada por error salvo restaurar un backup de MySQL.
- Sin auditoría/log de quién pausó o eliminó qué tienda y cuándo.

---

### Fase 12 — Bugs de las previews del asistente + primer módulo real gateado por plan ✅ (2026-09-16)

Sesión mixta: primero 3 bugs reales encontrados por el usuario probando el
asistente "Crear tienda" y "Apariencia" (Fase 10), después el primer
feature nuevo construido usando `Plan.enabledModules` (Fase 5) para algo
de verdad — hasta ahora era mecanismo sin ningún módulo real que gatear.

**Bug 1 — las 6 miniaturas de diseño se veían todas con el mismo color:**
`admin-appearance`/`tenant-wizard` pintaban las 6 miniaturas de layout con
el `draftBrandColor()` compartido (el de la ÚLTIMA tienda que se estuvo
viendo/editando) en vez del color real de cada plantilla — imposible
comparar diseños así. Se abrió la carpeta `templates/` del Escritorio
(los 6 temas de WordPress originales, ver Fase 10 Paso 7) y se sacó el
color de acento REAL de cada uno de sus `customizer`/`style.css` (no
inventado): Botiga `#212121`, Rife Free `#3957ff`, Shopper `#734f96`,
Shopping Cart `#f77426` (usado para "mercado", que consolida 3 temas —
confirmado con el usuario: "usá el de la plantilla, después el cliente
elige el color que quiera"), Minna/boutique sin archivo descargado
(aproximado a su estética editorial, `#1c1917`, el usuario lo aceptó).
`LAYOUTS` (`admin-appearance.component.ts`) ahora tiene `previewColor` por
layout y `layoutSwatchVars()` (nueva, exportada) genera la rampa fija de
cada uno — las miniaturas (grid chico de ambas pantallas) usan SIEMPRE su
propio color, nunca el que se esté editando. El modal "ver en grande" y el
resto de las previews grandes siguen usando el color en edición (tiene
sentido ahí: un solo diseño a la vez, sin comparación posible).

**Bug 2 — encabezado/pie de página no reaccionaban al color, y el pie de
página directamente no aparecía en ninguna preview:** `HeaderComponent`/
`FooterComponent` sólo leían `headerColor`/`footerColor`/`textColor` de
`SettingsService` (la config REAL ya guardada) — sin ningún input de
"borrador" como sí tenía `CatalogPageComponent` (`textColorOverride`/
`pageBgOverride`). Y ninguna de las previews (asistente, "Apariencia")
incluía `<app-header>`/`<app-footer>` — sólo `<app-catalog-page>` (hero +
grilla), así que el pie de página no podía aparecer aunque se lo pintara
bien. Agregado a ambos componentes: `headerColorOverride`/
`footerColorOverride`, `textColorOverride`, mismo patrón `undefined` = sin
tocar / cualquier otro valor pisa. Sumado `<app-header>`/`<app-footer>` a
las 3 previews grandes reales (asistente pasos 1-4, pantalla completa del
paso 5, modal "ver en grande" de Apariencia) — NO a las miniaturas chicas
del grid de diseño, que siguen siendo sólo `<app-catalog-page>` a
propósito (comparación de estructura, no la tienda completa). Verificado
en el navegador: cambiar "Encabezado" a rojo y "Pie de página" a azul
repinta ambos al instante, con el pie de página visible al final del
scroll.

**Bug 3 — el nombre/logo de una tienda nueva mostraban los de la tienda
piloto:** mismo problema de fondo que el Bug 2 — `CatalogPageComponent`
usaba `storeName()`/`logoSrc()` directo de `SettingsService` sin ningún
override (a diferencia de `logoOverride`, que sí existía pero sólo para
el logo YA subido a Cloudinary, no aplicaba mientras el asistente sigue en
los primeros pasos). Agregado `storeNameOverride` a `CatalogPageComponent`
y `storeNameOverride`/`logoOverride` a `Header`/`FooterComponent`,
cableados con `draft().name`/`logoDataUrl()` del wizard en las 3 previews
grandes Y en el grid chico del Paso 1. Sub-bug encontrado al verificar
esto: el "logo genérico" que se mostraba cuando todavía no se subió
ninguno en realidad era `LOGO_FALLBACK` (`'logo.jpeg'`) — el logo REAL de
Estilos Pequeños, usado a propósito como su propio fallback (la piloto no
tiene `logoUrl` en la fila de `SiteSettings`, ver Fase 9 ampliada #35) —
osea que una tienda nueva sin logo mostraba la marca de la piloto. Se
armó `GENERIC_LOGO_PLACEHOLDER` (nuevo, `core/utils/generic-logo.ts`): un
ícono 🏬 sobre círculo gris como SVG data URI, usado SÓLO en la rama de
override de los 3 componentes (`logoOverride() !== undefined`) — el
fallback real de la piloto (`LOGO_FALLBACK`, sin override, tenant ya
existente) queda intacto. Verificado en el navegador: tienda nueva
"La Tienda Nueva" en el asistente muestra su propio nombre en las 3
previews grandes Y en el grid chico, con el ícono genérico en vez del
logo de la piloto.

**Feature nuevo — "Publicar en redes" (compartir producto a Facebook/
Instagram):** pedido explícito del usuario: al crear/editar/listar un
producto, un botón que arme una publicación con la foto y un texto
(título, descripción, o texto libre) y la mande a redes. Discutido primero
el alcance real (varias idas y vueltas con el usuario, importante para
quien retome esto):
- **NO usa la API de Meta** (Graph API) — eso requeriría una cuenta
  Instagram Business conectada a una Página de Facebook Y una app de Meta
  **revisada y aprobada por Meta** (proceso externo de semanas, no
  depende del código). Se descartó a propósito.
- **Usa la Web Share API nativa del navegador** (`navigator.share` con
  `files`) — sin tokens, sin aprobación de nadie, pero por eso **sólo
  funciona desde el celular** que tiene Facebook/Instagram instalados y
  logueados: el botón abre el panel nativo de compartir del teléfono, el
  dueño de la tienda elige la app y publica con su propia cuenta (el
  código nunca ve ni guarda ninguna credencial de redes). **Desde PC no
  hay forma real** — ni con esto ni con la API de Meta se puede publicar
  en Instagram desde una computadora (limitación de la app, no de este
  proyecto); por eso el botón directamente no se muestra si
  `navigator.share` no existe (la nota "abrí este panel desde el celular"
  se muestra en su lugar, sólo en la variante completa del formulario, no
  en la compacta del listado).
- **Limitación conocida de Instagram** (no arreglable desde acá): al
  compartir a Instagram, la foto se abre lista pero el texto casi nunca se
  prellena en el cuadro de descripción — por eso, además de pasarlo en
  `ShareData.text`, se copia también al portapapeles (`navigator.
  clipboard.writeText`) como red de contención, para que se pueda pegar a
  mano.
- **Bug de activación de usuario, encontrado y arreglado en la misma
  sesión:** `navigator.share()` exige estar "manejando un gesto del
  usuario" (el click) — cualquier `await` de por medio puede consumirlo.
  La primera versión hacía `fetch` de la foto → `blob()` → **`await
  navigator.clipboard.writeText(...)`** → recién ahí `navigator.share()`,
  y ese `await` de más tiraba `NotAllowedError: Must be handling a user
  gesture` (confirmado en el navegador real, no sólo en teoría). Se
  reordenó: `navigator.share()` se llama apenas está listo el archivo
  (sólo `fetch`+`blob()` de por medio), y el `writeText` al portapapeles
  quedó DESPUÉS, sin `await` bloqueante (`.catch(() => {})`) — ya no hace
  falta la activación a esa altura. Verificado en el navegador: antes del
  fix, un click real tiraba el toast de error; después del fix, sin error
  (el share sheet en sí es UI nativa del SO, fuera del alcance de la
  automatización del navegador — falta la confirmación final en un
  celular real antes de dar esto por 100% cerrado).
- **Opciones de texto:** "Usar el título" / "Usar la descripción" /
  "Escribir otro texto" (textarea libre, para el caso que motivó el
  pedido: reflotar un producto viejo con una descripción nueva tipo
  "¡Volvió a estar disponible!"). Preview en vivo ("Así se vería": la foto
  + el texto elegido, se actualiza al tipear).
  **"Volver a publicar" ya funciona sin ningún cambio extra** — el botón
  no tiene ningún estado de "ya publicado" que lo bloquee; se puede
  compartir el mismo producto las veces que se quiera, en cualquier
  momento, desde el listado o el formulario.
- **`SocialShareButtonComponent`** (nuevo, `shared/components/`): variante
  completa (formulario, con el selector de texto + preview) y `compact`
  (listado, sólo un link de texto "📲 Publicar", usa el título como
  caption por defecto). Cableado en `admin-product-form` (crear y editar —
  funciona incluso antes de guardar el producto, porque las fotos ya están
  en Cloudinary apenas se suben) y en `admin-products` (una fila por
  producto). NO agregado a productos archivados (no tiene sentido
  promocionar algo fuera del catálogo).

**Modularización pedida explícitamente por el usuario ("ojo, hay que
modularizar para poder darle o no acceso al cliente según lo que pague"):**
primer uso real de `Plan.enabledModules` (existía desde la Fase 5, sin
gatear nada) — nuevo `com.saasweb.core.plan.Modules` (constantes de
clave de módulo, hoy sólo `SOCIAL_SHARE`), `Plan.hasModule(key)`.
`GET/PUT /api/settings` (público y admin) suma `socialShareEnabled`
(boolean, calculado server-side con `PlanService.getCurrent()`) — el
frontend (`SettingsService.settings().socialShareEnabled`) lo usa para
ocultar el componente entero si el plan no lo tiene habilitado. **Bug de
backfill encontrado y arreglado en la misma sesión:** `PlanService.
ensureDefault()` sólo se llamaba desde `TenantService.ensureDefault()`
**dentro de la rama `orElseGet` de creación del tenant** — con el tenant
piloto ya existente (cualquier arranque después del primero), esa rama
nunca corre, así que el plan ya sembrado nunca recibía el módulo nuevo
(quedó `enabledModules = {ropa}`, sin `SOCIAL_SHARE`, y el botón no
aparecía en ningún lado pese a estar todo el resto del código bien).
Arreglado en dos partes: `PlanService.ensureDefault()` ahora también
backfillea `SOCIAL_SHARE` al plan YA sembrado (no sólo al crearlo), y
`DataSeeder.run()` llama a `planService.ensureDefault()` directo (no sólo
implícito a través de `tenantService.ensureDefault()`) — mismo patrón que
`DataSeeder.backfillHeroSlidesAndLogos()`, corre en cada arranque.
Verificado con SQL directo (`plan_module`) y `GET /api/settings` antes y
después del fix: `{"ropa"}` → `{"ropa","SOCIAL_SHARE"}`,
`socialShareEnabled: false` → `true`.

**Ajustes pedidos por el usuario después de probarlo, mismo día:**
- **`navigator.share` NO alcanza para saber "es un celular"** — Windows
  10/11 con Chrome/Edge moderno también la implementa (abre el panel de
  Compartir del propio Windows), así que el botón aparecía igual en una
  PC. Sumada una segunda señal, `isLikelyMobileDevice()`
  (`navigator.userAgentData.mobile`, con fallback al string de
  user-agent) — el tamaño de pantalla/ventana se descartó a propósito
  (discutido con el usuario): no distingue "es un celular" de "la ventana
  está angosta", da falsos positivos/negativos en ambos sentidos.
- **El botón no debía desaparecer en PC, sólo avisar** — pedido explícito
  del usuario tras la primera versión (que ocultaba todo si
  `!supported`): mejor que el feature se descubra aunque hoy no se pueda
  usar ahí. Rediseñado: el botón queda SIEMPRE visible (mientras
  `moduleEnabled()`); si se aprieta sin `supported`, un toast avisa "esto
  sólo funciona desde el celular" en vez de abrir nada.
- **Elegir qué fotos mandar, no siempre la portada sola** — pedido tras
  preguntar el usuario qué pasaba con productos de varias fotos (antes
  mandaba sólo `images[0]`). `SocialShareButtonComponent` pasó de recibir
  un `imageUrl` único a `images: string[]` (todas): en la variante
  completa (formulario) aparece un selector de miniaturas ("¿Qué fotos
  publicar?", arranca con sólo la portada tildada, mínimo 1 siempre
  elegida) y la preview "Así se vería" muestra todas las elegidas
  superpuestas; `share()` hace `fetch` de cada una elegida y arma
  `files: File[]` con todas para `navigator.share`. Selección por URL, no
  por índice, para no romperse si se reordenan/sacan fotos en el medio.
  Documentado en el texto de ayuda: Instagram por este mecanismo
  generalmente sólo toma la primera aunque se manden varias (limitación
  de esa app), Facebook/WhatsApp sí arman álbum. La variante `compact`
  (listado) sigue mandando sólo la portada — no tiene lugar para un
  selector en una fila de tabla.
- Verificado todo en el navegador: en PC, con `userAgentData.mobile:
  false` confirmado por consola, el botón compacto y el completo quedan
  visibles y al tocarlos muestran el toast de aviso (no un error); en el
  formulario, elegir ambas fotos de un producto de 2 fotos las tilda a
  las dos y la preview las muestra juntas.

**NO hecho en esta fase:**
- Sin pantalla de administración de planes/módulos (asignar módulos a un
  plan sigue siendo un `UPDATE` a mano en `plan_module`, no hay UI —
  consistente con que sigue habiendo un solo plan real, ver Fase 5).
- Sin test automatizado para nada de esto.
- Sin confirmación en un celular real del flujo de compartir (sólo
  navegador de escritorio con Chrome DevTools Protocol, que sí expone
  `navigator.share` pero no hay forma de ver la hoja nativa de compartir
  del SO desde la automatización).
- **Mercado Pago (pedido, discutido, NO empezado):** el usuario pidió
  después, en la misma sesión, un módulo de checkout con pago online real
  vía Mercado Pago (carrito completo, y que el stock se descuente recién
  cuando el pago esté confirmado — no al hacer el pedido como hoy),
  también modularizado para poder ofrecer WhatsApp o pago online según el
  plan. Es un pedido legítimo y del mismo patrón (`Modules`), pero de un
  orden de magnitud bien distinto: plata real, credenciales de Mercado
  Pago que hay que conseguir, un webhook que hay que exponer y validar
  (firma, idempotencia), y cambiar el momento en que se descuenta stock
  (hoy es al confirmar el pedido, no al pagarlo) — se le señaló al usuario
  que esto merece su propia sesión de planificación en vez de sumarlo de
  apuro a esta. Queda pendiente, no diseñado todavía.

---

### Fase 13 — Checkout con pago online real (Mercado Pago) 🔜 (2026-09-16, backend + frontend construidos, falta la prueba real)

Pedido explícito del usuario en la misma sesión de la Fase 12: carrito
completo con pago online a través de Mercado Pago, con el stock
descontándose recién cuando el pago está validado (no al hacer el
pedido, como hoy) — y modularizado (mismo mecanismo de `Plan.
enabledModules` que Fase 12) para poder ofrecerlo o no según lo que
pague cada tenant.

**Decisión de modelo de integración — discutida con el usuario:**
Mercado Pago tiene dos caminos. "Checkout API — Orders" (el más nuevo)
requiere armar un formulario de tarjeta propio en el sitio, tokenizado
con el SDK de MP en el navegador — checkout embebido, más superficie de
riesgo y trabajo (cuotas, marcas de tarjeta, validaciones a mano).
"Checkout Pro" (clásico) arma una "preferencia" con los ítems y
redirige al cliente a una página 100% de Mercado Pago — tarjeta,
cuotas, validación, todo lo construye y lo aloja Mercado Pago, nuestro
código nunca ve un número de tarjeta ni siquiera tokenizado. Se eligió
**Checkout Pro** por ser sustancialmente más simple y seguro para este
proyecto — la confirmación por webhook es igual en los dos modelos, así
que no se pierde la parte que le importaba al usuario (validar el pago
antes de descontar stock). Queda documentado por si en el futuro se
quiere migrar al checkout embebido — el webhook y la lógica de
confirmación automática no cambian.

**Módulo (mismo mecanismo que Fase 12):** `Modules.MERCADOPAGO` +
backfill en `PlanService.ensureDefault()` (ahora generalizado a un
`Set<String> knownModules` en vez de un `if` por módulo, para no seguir
repitiendo el patrón a mano cada vez).

**Credenciales — POR TENANT, no de plataforma** (a diferencia de
Cloudinary): cada tienda cobra a su propia cuenta de Mercado Pago.
`SiteSettings` suma `mpEnabled` (boolean), `mpAccessToken` (secreto de
verdad — mismo criterio que `smtpPassword`, nunca se devuelve en
ninguna respuesta, sólo `accessTokenSet`) y `mpPublicKey` (no secreta).
Nuevos `MercadoPagoConfigRequest`/`Response` + `GET`/`PUT
/api/admin/settings/mercadopago`, gateados `PAYMENTS_MANAGE` (el admin
normal de la tienda, no sólo superadmin — es SU cuenta). Pantalla nueva
`/admin/config/mercadopago` (mismo patrón que la de SMTP tenant:
`accessTokenSet` en vez de mostrar el token guardado).

**`GET /api/settings` suma `mercadoPagoAvailable`** (boolean,
calculado server-side: módulo del plan Y `mpEnabled` Y `mpAccessToken`
cargado) — el frontend lo usa para decidir si ofrecer "Pagar con
Mercado Pago".

**Regla de negocio pedida explícitamente por el usuario: si Mercado
Pago está activo, es el ÚNICO medio de pago que se ofrece en el
carrito online** (no conviven con transferencia/QR/efectivo ahí — para
no mezclar un pago validado automáticamente con medios que dependen de
coordinar a mano por WhatsApp). Implementado en
`SettingsService.availablePaymentMethods` (frontend): si
`mercadoPagoAvailable`, devuelve sólo `['MERCADOPAGO']`, ignorando el
resto de los flags. **"Venta en el local" (`admin-pos`) NO se ve
afectada** — sigue ofreciendo los 5 medios siempre (efectivo,
transferencia, QR transferencia, QR/link tarjeta, y ahora también
Mercado Pago como una etiqueta más, para cuando el vendedor cobra con
MP en persona) — tiene su propia lista fija, nunca leyó
`availablePaymentMethods`.

**Modelo de datos (`Order`):** nuevos `paymentStatus` (enum
`PENDING`/`APPROVED`/`REJECTED`, nuevo — **separado a propósito** de
`OrderStatus`, que sigue siendo el flujo manual de siempre; `null` para
cualquier medio de pago que no sea Mercado Pago), `mpPreferenceId`,
`mpCheckoutUrl` (el link al que se redirige — se guarda para poder
reofrecerlo si el cliente no pagó todavía), `mpPaymentId`. Migró solo
(Hibernate `ddl-auto: update`), verificado con `DESCRIBE orders`.

**Flujo completo:**
1. Checkout público (`POST /api/orders`) ahora llama a
   `OrderService.createWebCheckout()` (nuevo — NO `POST /api/admin/
   orders/pos`, que sigue llamando a `create()` a secas): crea el
   pedido normal (`status=PENDIENTE`, stock sin tocar, igual que
   siempre) y, si `paymentMethod=MERCADOPAGO`, arranca
   `startMercadoPagoCheckout()` — arma la preferencia vía
   `MercadoPagoService.createPreference()` (nuevo, `core/payment/`,
   usa `Spring RestClient`, primer cliente HTTP saliente del backend —
   no había ninguno antes, Cloudinary se sube directo desde el
   navegador y el mail es SMTP) con el Access Token DEL TENANT,
   `external_reference = order.id`, `notification_url` con
   `?tenantId=...` en la query string (mecanismo de resolución de
   tenant del webhook, ver abajo), y `back_urls` a `/mis-pedidos?code=
   ...`. Guarda `mpPreferenceId`/`mpCheckoutUrl`/`paymentStatus=PENDING`
   en el pedido. **Todo dentro de la misma transacción que crear el
   pedido** — si la preferencia falla (token inválido, Mercado Pago
   caído), el pedido entero se revierte, no queda un pedido roto sin
   forma de pagarlo. **Verificado con un token falso**: error 403 de
   Mercado Pago → mensaje claro en el carrito ("No se pudo iniciar el
   pago... revisá el Access Token") → confirmado por SQL que no quedó
   ningún pedido húérfano en `orders`.
2. El frontend (`cart-page.component.ts`), si `paymentMethod ===
   'MERCADOPAGO'` y vino `mpCheckoutUrl`, limpia el carrito y hace
   `window.location.href = mpCheckoutUrl` — saca al cliente del sitio
   directo a la página de Mercado Pago (no abre WhatsApp, ese flujo
   entero queda sin usar para este medio de pago). Botón y textos del
   carrito cambian según `isMercadoPagoOnly()`.
3. **`POST /api/webhooks/mercadopago`** (nuevo, `core/payment/
   MercadoPagoWebhookController`, público, sin JWT): resuelve el tenant
   del query param `?tenantId=...` de la URL que se registró (**nunca**
   del `TenantContext` que ya puso `TenantResolutionFilter` antes de
   llegar acá, que resuelve "el único tenant activo" por defecto —
   irrelevante para un webhook que es de un tenant específico). Saca el
   id del pago del body (`{type, data: {id}}`, formato moderno) o de
   query params como respaldo. **Nunca confía en el cuerpo de la
   notificación para el estado real** — vuelve a pedirle el pago a la
   API de Mercado Pago (`GET /v1/payments/{id}`, autenticado con el
   Access Token del tenant) para no poder ser falseado. Si
   `status=approved` → `OrderService.confirmFromPayment()` (nuevo —
   mismo descuento de stock que la confirmación manual del panel,
   `doConfirm()` extraído como privado y compartido entre las dos,
   registrado como "Mercado Pago (pago validado)" en vez de un DNI). Si
   `rejected`/`cancelled` → `markPaymentRejected()` (cancela el pedido
   — nunca se tocó el stock, cancelar es seguro). Cualquier excepción
   al aplicar el pago (ej. se quedó sin stock justo antes de que se
   apruebe) queda sólo logueada, **siempre responde 200** — Mercado
   Pago reintenta agresivo ante cualquier respuesta que no sea 2xx, y
   reintentar no soluciona un problema de stock.
4. `/mis-pedidos` (consulta pública) muestra el estado del pago y,
   si sigue `PENDING`, un botón "Terminar de pagar" con el
   `mpCheckoutUrl` guardado — no hace falta que el cliente vuelva a
   armar el pedido si no llegó a pagar la primera vez.

**Bug evitado antes de escribirlo mal (encontrado pensando el pedido
del usuario "en el local también podría recibir Mercado Pago"):** si
`OrderService.create()` hubiera arrancado el checkout online para
CUALQUIER pedido con `paymentMethod=MERCADOPAGO` sin importar el canal,
una venta de "Venta en el local" marcada como "me pagaron con Mercado
Pago en el momento" (una etiqueta nada más, no un pago real que haya
que validar) habría intentado crear una preferencia y un link de pago
sin sentido. Por eso `create()` en sí NO toca Mercado Pago — sólo
`createWebCheckout()` (checkout público) lo hace; `createPos()` sigue
llamando a `create()` a secas.

**`AppProperties`/`application.yml` suma `app.urls.backend`/
`app.urls.frontend`** (`BACKEND_PUBLIC_URL`/`FRONTEND_URL`, default
`localhost`) — hacían falta para armar `notification_url`/`back_urls`
absolutas; no existía ningún concepto de "URL pública del deploy" en
el proyecto antes (los mails de recuperación de cuenta mandan una
contraseña nueva en texto, no un link).

**Verificado en el navegador (con un Access Token de prueba FALSO —
todavía no real):**
- Pantalla `/admin/config/mercadopago`: activar + guardar token +
  `GET /api/settings` confirma `mercadoPagoAvailable: true`.
- Carrito público: con Mercado Pago activo, "¿Cómo vas a pagar?"
  muestra sólo "Mercado Pago" (el resto desaparece), botón cambia a
  "🅿️ Pagar con Mercado Pago".
- Enviar el pedido con el token falso: error 403 de Mercado Pago
  mostrado claro en el carrito, sin pedido huérfano en la base
  (confirmado por SQL) — la transacción revirtió todo correctamente.
- Desactivado el token falso al terminar para no dejar el checkout de
  la tienda piloto roto.

**2026-09-16 (continuación tras corte por límite de tokens):** cerrados
los dos pendientes que no dependían de la cuenta real de Mercado Pago:
- `admin-order-detail` (panel) ahora muestra el estado del pago debajo
  de "Forma de pago" cuando `paymentMethod === 'MERCADOPAGO'` (mismo
  patrón visual que "Mis pedidos" público: ⏳ pendiente / ✓ acreditado /
  ✕ rechazado), leyendo `order.paymentStatus` que ya venía en el DTO
  pero no se mostraba en ningún lado del panel. No se agregó
  `mpPreferenceId` a la vista — es un id técnico interno sin utilidad
  para el dueño de la tienda, no vale la pena exponerlo.
- Arreglado el bug cosmético del banner "Promos disponibles" del
  carrito: `promoHints()` (`cart-page.component.ts`) ahora omite los
  hints de descuento por medio de pago (`Pagando con Transferencia o
  Efectivo: X% off`) cuando `isMercadoPagoOnly()` es true, porque esos
  medios dejan de ser elegibles en ese caso.
- **Verificado en el navegador** (no sólo compilado): los 3 estados de
  `paymentStatus` en `admin-order-detail` se probaron pisando
  temporalmente `payment_method`/`payment_status` por SQL directo en un
  pedido de prueba real (`PED-0002`) y revirtiendo el UPDATE al
  terminar — no se creó un pedido de Mercado Pago real porque todavía
  no hay Access Token de verdad (ver abajo). El fix del banner se
  verificó con Mercado Pago activo de verdad en el carrito público: la
  línea de "Pagando con Transferencia..." ya no aparece y "¿Cómo vas a
  pagar?" sigue mostrando sólo Mercado Pago, sin regresión.

**LO QUE FALTA — sigue bloqueado por el lado de Mercado Pago, no es
código pendiente:**
- **La prueba real con un Access Token de prueba de verdad** (el
  usuario está sacando su cuenta de Mercado Pago) — sin eso no se vio
  ni una vez la página de pago real de Mercado Pago (tarjeta, cuotas,
  total), sólo el rechazo del token falso.
- **El webhook nunca recibió una notificación real** — Mercado Pago no
  puede pegarle a `localhost:8080`; hace falta un túnel (ngrok o
  similar) para probarlo desde una compra de prueba real, o simularlo
  a mano con `curl` contra el endpoint.
- Sin test automatizado para nada de esta fase.

---

### Fase 14 — Modo Kiosco/POS + Facturación ARCA (2026-09-16)

Pedido nuevo del usuario, en la misma sesión de la ronda de mejoras
post-Fase 13: además de tiendas con vidriera online, la plataforma
tiene que poder ofrecer un **punto de venta puro** (kiosco, casa de
repuestos) — negocios que sólo venden presencial, con lector de
código de barras, control de stock y facturación (interna y AFIP/ARCA
real, las dos), **en el mismo backend** que el resto — no un producto
aparte.

**Decisión de arquitectura — discutida con el usuario:** en vez de un
flag rígido "tienda sí/tienda no" en `Tenant`, se reutiliza el
mecanismo que ya existe, `Plan.enabledModules` — el sitio público en
sí pasa a ser un módulo más (`ECOMMERCE_SITE`, todavía no creado) y se
suma un módulo `POS`. Un tenant puede tener uno, el otro, o los dos —
mismo backend, misma base, mismo login de admin. Esto además hace útil
de entrada el panel de Planes (Fase de hoy, más arriba): el día de
mañana se arman planes tipo "Sólo Web" / "Sólo POS" / "Combo" desde
ahí, sin tocar código.

**Facturación (Fase D, más grande, todavía sin arrancar):** el usuario
confirmó que quiere **las dos** — ticket interno (no fiscal) y Factura
AFIP/ARCA real (A/B/C). Para AFIP se descartó integrar directo con el
web service (certificado, homologación, manejo de contingencias — un
proyecto regulatorio en sí mismo) a favor de un proveedor tercero que
ya envuelve eso en una API REST (candidatos relevados pero sin elegir
todavía: TusFacturas, iFactura, PyAfipWs). Nota para cuando se retome:
en 2026 ARCA exige QR + CAE en formato nuevo en cada factura — un
motivo más para dejarlo del lado del proveedor.

**Lo que se hizo hoy (lo que no dependía de la decisión de
facturación):**
- `Product.barcode` (nuevo, opcional, aditivo) — código de barras real
  (EAN/UPC de fábrica), distinto del QR propio de la tienda que ya
  generaba `admin-product-qr`. Campo nuevo en el form de producto,
  sección "Compra/proveedor".
- Búsqueda por código de barras en **Venta en el local**, sumada a la
  búsqueda por nombre existente (no la reemplaza). Si lo que se tipeó
  matchea EXACTO el barcode de un producto con un solo talle, lo suma
  directo al carrito sin clickear nada (flujo "beep, beep, beep" de un
  lector USB, que escribe rápido y termina en Enter — no hace falta
  ningún SDK ni hardware especial). Si el producto tiene varios talles,
  no hay forma de adivinar cuál — queda filtrado en la grilla para
  elegirlo a mano, igual que con la búsqueda por nombre.
- **Medio de pago `POSNET`** nuevo (tarjeta con la máquina física del
  local — sin integración real con el posnet, sólo se anota el ticket).
- **Campos nuevos en `Order`**: `amountTendered` (con cuánto pagó el
  cliente en efectivo — el backend calcula que alcance para el total y
  rechaza si no) y `paymentReference` (un campo genérico que significa
  algo distinto según el medio: con `TRANSFER`, nombre y apellido de
  quien transfirió — para cruzarlo después con el resumen bancario; con
  `POSNET`, el número de ticket que imprime la máquina al aprobar). Sin
  integración real con ningún banco ni posnet — son notas a mano, tal
  como lo pidió el usuario describiendo lo que ya hacen los comercios
  reales.
- **Venta en el local**: con Efectivo, un campo "Con cuánto paga"
  (opcional) que calcula y muestra el vuelto en el momento; con
  Transferencia, el campo de nombre de quien transfirió; con Posnet, el
  campo de número de ticket. Vuelto/referencia se muestran también en
  el recibo imprimible y en el detalle de pedido del panel.

**Verificado en el navegador de punta a punta** (no sólo compilado):
producto real (`Zapatillas urbanas velcro`) editado con un código de
barras de prueba; en Venta en el local, escanearlo (tipear + Enter)
filtró la grilla a ese producto (no lo auto-agregó, porque tiene 6
talles — comportamiento esperado, confirma que la guarda de "un solo
talle" funciona); agregado a mano, probado Efectivo con $30.000 contra
un total de $24.750 → "Vuelto: $5.250" correcto; cambiado a
Transferencia y a Posnet, confirmando que el campo cambia de nombre y
de sentido en cada caso; venta registrada con Posnet + ticket "004521"
→ confirmado en el recibo impreso y en el detalle del pedido del panel
que ambos datos (medio de pago y N° de ticket) se ven bien. **Nota:**
el auto-agregado por barcode exacto con un producto de un solo talle
no se verificó en el navegador (hubiera hecho falta armar un producto
de prueba nuevo sólo para eso) — la lógica es un condicional simple ya
tipado, y llama a la misma función `addProduct(...)` ya probada por el
flujo manual de "tocar un talle".

**Corrección de rumbo, misma sesión:** el usuario marcó que lo de
arriba (barcode/posnet/vuelto) se metió directo en "Venta en el
local", la pantalla que ya usa la tienda piloto (ecommerce) — mezclando
la lógica de un tenant con sitio online con la de un negocio
puramente presencial, cuando la idea original era que fueran dos
lógicas/templates separados. Se resolvió así:
- **Módulos `ECOMMERCE_SITE` y `POS` reales** en `Modules.java`
  (backfill al plan ya sembrado, como todos los módulos anteriores).
  `TenantResolutionFilter` ahora bloquea las rutas públicas (mismo
  criterio y mismo response que una tienda pausada) para un tenant sin
  `ECOMMERCE_SITE` — un tenant sólo-POS no tiene vidriera, sólo panel.
  `GET /api/settings` suma `posEnabled`/`ecommerceSiteEnabled`
  (mismo patrón que `socialShareEnabled`/`mercadoPagoAvailable`) para
  que el panel sepa qué mostrar.
- **Pantalla nueva y separada**: `admin-kiosco` (ruta
  `/admin/kiosco`, "Punto de venta (kiosco)" en el menú, visible sólo
  si `posEnabled`) — comparte los SERVICIOS de backend
  (`ProductService`/`OrderService`/`DiscountService`) pero es un
  componente propio, con su propio layout de "caja registradora": sin
  cupón, sin email de marketing, sin delivery, y sólo los 3 medios de
  pago que se cobran parado en un mostrador (Efectivo/Transferencia/
  Posnet) — nada de QR/Mercado Pago, que son cosas de ecommerce.
  "Venta en el local" (`admin-pos`) sigue existiendo tal cual para
  tiendas que SÍ tienen ecommerce y también venden en persona — no se
  le sacó nada de lo que se le había sumado antes en la misma sesión
  (barcode, posnet, vuelto le quedan bien puestos igual, porque
  cualquier venta presencial los puede necesitar tenga o no sitio
  online).
- Sumados los 2 módulos nuevos a la pantalla de Planes (Fase de hoy)
  para poder prenderlos/apagarlos sin SQL — hasta ahora sólo tenía los
  2 módulos viejos.
- **Verificado en el navegador**: `/admin/kiosco` funciona de punta a
  punta con su propio layout (buscador auto-enfocado arriba, sin cupón
  ni email, sólo 3 medios de pago) — escaneado el mismo barcode de
  prueba, agregado un talle, probado Efectivo ($30.000 contra
  $24.750 → vuelto $5.250 correcto) y registrada la venta, confirmado
  en el recibo. Apagado el módulo `POS` desde Planes → el ítem
  "Punto de venta (kiosco)" desaparece del menú al instante; vuelto a
  prender y confirmado por SQL que los 5 módulos del plan real (el
  único que existe, usado por las 9 tiendas) quedaron exactamente como
  antes. **No** se probó apagar `ECOMMERCE_SITE` contra la tienda
  piloto real — hubiera significado bloquear su sitio público en vivo
  para probarlo; el código es un espejo directo del mismo mecanismo de
  "tienda pausada" que ya está en producción y probado, así que se
  aceptó ese riesgo como innecesario de verificar hoy.

**Menú adaptado + paso del asistente para elegir plan, misma sesión.**
El usuario pidió puntualmente estas dos cosas, con una restricción
clara para la segunda: el plan se elige entre planes YA CREADOS desde
"Planes" (no que el asistente arme uno nuevo sobre la marcha) — más
simple y alcanza de sobra mientras sea el propio usuario el único que
da de alta tiendas.
- **Menú**: los items puramente de ecommerce (Venta en el local,
  Cupones, Campañas, Vista general, Mensaje de WhatsApp, Medios de
  pago, Mercado Pago, Redes sociales, Sobre nosotros, Cómo comprar+FAQ,
  Carrusel, Página de inicio) ahora se ocultan cuando el tenant no
  tiene `ECOMMERCE_SITE`. Lo universal (Pedidos, Cambios, Caja, Turnos,
  Descuentos, todo Catálogo, Identidad y contacto, Servicio de mail,
  Usuarios y roles) queda siempre visible — lo necesita cualquier
  negocio, tenga o no sitio online.
- **Asistente "Crear tienda"**: nuevo selector de Plan al principio del
  Paso 1 (sólo se muestra si hay más de uno — hoy con un solo plan no
  cambia nada visible), con el resumen de módulos de cada plan
  ("Publicar en redes · Mercado Pago · Sitio web · Punto de venta") para
  distinguirlos de un vistazo. `TenantService.create` ahora recibe y
  valida el `planId` elegido (rechaza uno inexistente).
- **Bug real encontrado y arreglado verificando en el navegador**: al
  apagar `ECOMMERCE_SITE` para probar el menú, el PROPIO panel de
  administración se rompió — el listado de Productos tiraba "Esta
  tienda no tiene sitio online" y la columna Parametrías quedaba vacía.
  Causa: varias pantallas del panel (ej. ese listado, que resuelve
  nombres de parametría vía `/api/param-groups`) pegan a endpoints
  técnicamente "públicos" (`/api/products`, `/api/param-groups`,
  `/api/settings`, `/api/size-scales`...) reusados también por el sitio
  real, en vez de duplicarlos bajo `/api/admin/`. El gate de
  `TenantResolutionFilter` (y el de "tienda pausada", que tenía el
  mismo problema latente sin que nadie lo hubiera notado) sólo eximía
  `/api/admin/**`/`/api/auth/**`, así que bloqueaba también esas
  llamadas legítimas del panel. Arreglado en dos partes: el filtro
  ahora también exime cualquier request que traiga un
  `Authorization: Bearer` (no hace falta validarlo ahí, sólo saber que
  no es un visitante anónimo — `JwtAuthFilter`, que sí valida, corre
  después); y el interceptor del frontend (`auth.interceptor.ts`), que
  antes sólo mandaba el token a `/api/admin/**`, ahora lo manda a
  cualquier request a nuestro propio backend. Verificado de nuevo en el
  navegador después del fix: apagado `ECOMMERCE_SITE`, el panel navegó
  sin errores (Parametrías con datos reales, menú adaptado
  correctamente), reactivado y confirmado por SQL que el plan real (el
  único, usado por las 9 tiendas) quedó exactamente como estaba.
- **Selector de plan verificado de punta a punta**: creado un plan de
  prueba temporal por SQL ("Plan Kiosco (prueba)", sólo módulo `POS`),
  abierto el asistente, confirmado que aparecen las 2 opciones con su
  resumen de módulos, elegido el de prueba, creada una tienda
  descartable, confirmado por SQL que `tenant.plan_id` quedó apuntando
  al plan elegido (no al default) — borrada la tienda de prueba desde
  el panel (con confirmación de slug) y el plan de prueba por SQL al
  terminar.

**Facturación real: ticket interno + Factura C de ARCA, integración
directa (sin proveedor tercero), misma sesión.** El usuario pidió
puntualmente "usá la API específica de ARCA" en vez del proveedor
tercero que había quedado anotado como pendiente de elegir más arriba
— cambio de rumbo respecto de esa nota. Investigado por WebSearch antes
de tocar código (con fuentes citadas en el momento):
- **No existe API REST/JSON de ARCA** — el mecanismo real son dos
  servicios SOAP/XML viejos pero vigentes: **WSAA** (autenticación:
  arma un XML "login ticket request", lo firma como CMS/PKCS#7 con el
  certificado+clave privada del contribuyente, y a cambio devuelve un
  Token+Sign válido ~12hs) y **WSFEv1** (el de negocio: pide el último
  comprobante autorizado y pide el CAE del nuevo). No hay SDK oficial
  en Java moderno — se construyó a mano.
- **RG 4290 (2021) confirma lo que el usuario preguntó sobre
  impresoras fiscales**: no son obligatorias si se emite Factura
  Electrónica en su lugar — es un sustituto legal completo, no hace
  falta hardware fiscal para vender en el local.
- **Sí existe entorno de homologación (pruebas)** de ARCA, con sus
  propias URLs de WSAA/WSFE y CUIT/certificado de prueba — confirmado
  lo que el usuario intuía.
- **Alcance deliberadamente acotado para el MVP**: sólo **Factura C**
  (el usuario es Monotributo/Exento típico — no discrimina IVA) y sólo
  **consumidor final**, sin detalle de ítems (WSFEv1 sin
  `FeDetReq`/líneas, sólo importe total) — documentado como decisión
  explícita en el javadoc de `ArcaWsfeClient`, no como limitación
  descubierta después. Factura A/B (Responsable Inscripto, con
  discriminación de IVA e ítems) queda afuera, a propósito.
- **Backend**: `bcpkix-jdk18on` (Bouncy Castle) sumado por Maven — el
  JDK no trae soporte para firmar CMS/PKCS#7, que exige WSAA.
  `ArcaWsaaClient` (arma y firma el TRA, cachea el Token+Sign por
  tenant con margen de 5 min antes del vencimiento real) y
  `ArcaWsfeClient` (`FECompUltimoAutorizado` + `FECAESolicitar`, XML
  armado a mano con text blocks, parseado con XPath) — ambos
  `package-private`, sólo los usa `ArcaInvoiceService` (público), que
  orquesta todo y arma también el QR obligatorio (RG 4892, URL
  `https://www.afip.gob.ar/fe/qr/?p=<json en base64>`). **Nunca tira
  excepción hacia afuera** — devuelve un resultado con `aprobado`/
  `error`, para que la venta nunca se caiga por un problema de ARCA.
  Nueva config por tienda (`SiteSettings`: CUIT, punto de venta,
  condición IVA, certificado/clave en PEM, modo prueba/producción,
  `invoiceMode` TICKET_INTERNO/FACTURA_ARCA) con el mismo patrón de
  campo-secreto ya usado para Mercado Pago (nunca se devuelve el valor
  guardado, sólo un booleano `xSet`; dejar el campo vacío al editar =
  no tocarlo). `OrderService.confirm()` llama a `applyInvoicing()` sólo
  para ventas `LOCAL`: por defecto ticket interno; si el tenant tiene
  `invoiceMode=FACTURA_ARCA` configurado y disponible, intenta la
  Factura C — si ARCA la rechaza o falla la conexión, la venta **no se
  cae** (ya se confirmó y descontó stock), queda como ticket interno
  con el error anotado en `Order.invoiceError` para reintentar a mano
  más adelante.
- **Pantalla de configuración nueva** `/admin/config/arca`
  ("Facturación (ARCA)", permiso `PAYMENTS_MANAGE`, visible sólo con
  módulo POS): elegir Ticket interno vs. Factura ARCA, modo prueba,
  CUIT, punto de venta, condición IVA, certificado y clave privada
  (textareas, enmascarados una vez guardados). Módulo nuevo
  `ARCA_INVOICING` sumado a `Modules`/pantalla de Planes, igual que
  los anteriores. (Nota: en la primera versión de esta pantalla,
  Responsable Inscripto tenía un aviso de "sólo Factura C, A/B no
  construido" — dejó de ser cierto con el cambio de más abajo, el
  texto ya está actualizado.)
- **Frontend del punto de venta**: no hace falta elegir Ticket/Factura
  por venta — es una configuración del tenant, no una decisión del
  cajero en cada cobro (más simple, como pidió el usuario). El kiosco
  muestra abajo del botón "Cobrar" qué va a emitir cada venta ("🧾
  Emite Factura C (ARCA)" / "📄 Emite ticket interno"), y el recibo
  imprimible (`admin-receipt`) y el detalle de pedido
  (`admin-order-detail`) muestran el resultado real después de cobrar:
  CAE + vencimiento + N° de comprobante + QR de AFIP si salió Factura,
  o el aviso de ticket interno (con el motivo del rechazo de ARCA si
  lo hubo) si no.
- **Única incertidumbre real, marcada en el código**: WSAA
  históricamente firmaba con SHA-1; la implementación usa SHA-256
  (`SHA256withRSA`) siguiendo la documentación vigente, pero queda
  comentado en `ArcaWsaaClient` como lo primero a revisar si falla un
  login real, porque es imposible de verificar sin un CUIT y
  certificado de homologación reales del usuario.
- **Verificado**: `mvn test` (todos los tests existentes, incluido el
  nuevo mock de confirmación con Mercado Pago) y `tsc --noEmit` del
  frontend, ambos limpios. **No verificado en el navegador ni con ARCA
  real** — no hay certificado/CUIT de homologación disponibles en esta
  sesión; falta que el usuario cargue sus propios datos de prueba en
  la pantalla nueva y haga una venta con Factura ARCA activada para
  confirmar el flujo de punta a punta (mismo criterio ya aplicado a
  Mercado Pago en su momento: se construye completo, se prueba en
  cuanto existan credenciales reales).

**Factura A/B + reintento manual, misma sesión (segunda ronda sobre lo
de arriba).** El usuario pidió puntualmente sumar estas dos cosas que
habían quedado en "LO QUE FALTA":
- **Factura A/B para Responsable Inscripto**: `ArcaInvoiceService`
  ahora elige el tipo de comprobante según `SiteSettings.arcaCondicionIva`
  — Monotributo/Exento sigue yendo por Factura C (como antes, sin
  discriminar IVA); Responsable Inscripto pasa a Factura B (consumidor
  final) o Factura A si se cargó el CUIT del comprador al cobrar.
  `ArcaWsfeClient` suma los tipos 1 (A) y 6 (B) y arma el array
  `<Iva>` que exige WSFEv1 para esos dos (Factura C no lo lleva).
  **Simplificación deliberada, documentada en el código**: una sola
  alícuota, 21% (IVA general) — `impNeto = total / 1.21`,
  `impIva = total - impNeto`. No hay forma de manejar productos con
  otra alícuota (10.5%, exentos puntuales) sin sumar esa info al
  catálogo, que no existe hoy — igual que antes, sigue siendo un solo
  importe total, sin ítems detallados (`FeDetReq`).
- **CUIT del comprador opcional**: campo nuevo en el punto de venta
  (`admin-kiosco` y también `admin-pos`/"Venta en el local", porque
  las dos crean ventas canal `LOCAL` y las dos pasan por el mismo
  `applyInvoicing`) — sólo aparece si la tienda es Responsable
  Inscripto con Factura ARCA activada. Se guarda en
  `Order.invoiceBuyerCuit` para que quede registrado en qué factura se
  usó. Sin CUIT cargado, sale Factura B igual (consumidor final) — el
  campo nunca bloquea la venta.
- **Reintento manual**: `POST /api/admin/orders/{id}/retry-invoice`
  (`ORDERS_MANAGE`) — vuelve a intentar `applyInvoicing()` sobre un
  pedido `LOCAL`/`PROCESADO` que quedó en ticket interno (por rechazo
  de ARCA, por falla de conexión, o porque en el momento de la venta
  la tienda todavía no tenía ARCA configurado). No toca stock ni
  cobra de nuevo. Botón "🧾 Intentar facturar con ARCA" en el detalle
  de pedido, visible sólo si `arcaAvailable` y el pedido sigue en
  ticket interno.
- **Verificado**: `mvn test` y `tsc --noEmit` limpios otra vez después
  de este segundo cambio. **Sigue sin poder probarse contra ARCA real**
  (mismo motivo que arriba: no hay CUIT/certificado de homologación en
  esta sesión) — el armado del XML con `<Iva>` para A/B es nuevo
  respecto de lo ya construido y no se verificó contra el servidor
  real de ARCA, a diferencia de la mecánica de WSAA/Factura C que sí
  quedó descripta arriba con su propia incertidumbre (SHA-256 vs
  SHA-1).

**LO QUE FALTA:**
- Probar con credenciales reales de ARCA (homologación) — confirmar el
  algoritmo de firma de WSAA (SHA-256 vs SHA-1) y el armado del XML
  con IVA discriminado para Factura A/B (ver arriba, las dos
  incertidumbres reales de esta fase).
- Múltiples alícuotas de IVA (hoy asume 21% general para todo el
  carrito) — hace falta que el catálogo sepa la alícuota por producto,
  que no existe todavía.
- Ítems detallados en el comprobante (`FeDetReq`) — sigue siendo un
  solo importe total en los tres tipos de factura.
- Sin test automatizado para nada de esta fase (ni kiosco ni ARCA).

---

## 5. Historial

- **2026-09-16**: panel para asignar módulos a un plan — tercer ítem de la
  ronda de mejoras (ver el de recuperación de contraseña, más abajo, para
  el contexto completo). Reemplaza el `UPDATE` a mano en `plan_module`
  que era el único mecanismo hasta ahora (ver PLAN_SAAS.md Fase 5/12).
  `PlanController` nuevo (`/api/admin/plans`, sólo `SUPERADMIN`, mismo
  criterio que `TenantController`: es un catálogo de plataforma, no algo
  que edite el admin de una tienda) con listado y edición (nombre,
  límites de productos/usuarios, módulos habilitados, branding de
  plataforma). Pantalla nueva `/admin/superadmin/planes`: una tarjeta
  editable por plan (hoy sólo existe el "default", pero el diseño ya
  soporta varios) con checkboxes por módulo — no hay ABM para crear
  planes nuevos todavía, sigue siendo dato (INSERT), no código, porque
  sólo hace falta uno por ahora.
  **Dos bugs reales encontrados y arreglados verificando en el
  navegador** (no sólo compilado):
  1. El botón "Guardar cambios" quedaba habilitado después de guardar
     con éxito — `dirty` estaba armado como un `computed()` que leía un
     campo `touched` plano (no una signal), así que mutar `touched`
     directo en el callback de éxito no disparaba una recomputación.
     Arreglado usando una signal de verdad para `touched`.
  2. El primer intento de guardar "Máx. productos" tiraba
     `TypeError: raw.trim is not a function` y la request nunca salía:
     un `<input type="number">` con `ngModelChange` manda el valor ya
     convertido a `number`, no el string del input — el código asumía
     siempre string. Arreglado coercionando con `String(...)` tanto al
     guardar el draft como al parsear el límite.
  **Verificado de punta a punta contra el plan real** (el único que
  existe, usado por las 9 tiendas — piloto incluida): cambiado
  `maxProducts` a 50 y destildado "Publicar en redes", confirmado por
  SQL que persistió de verdad (no sólo en la UI), y revertido al estado
  original (`sin límite`, los 3 módulos que ya tenía) al terminar —
  confirmado por SQL de nuevo que quedó exactamente como estaba antes.
- **2026-09-16**: segundo ítem de la ronda de mejoras (ver el de
  recuperación de contraseña, más abajo, para el contexto completo) —
  los errores del webhook de Mercado Pago (ej. "el cliente pagó pero se
  quedó sin stock justo antes de que se apruebe") quedaban SÓLO en un log
  del servidor: nadie del lado de la tienda se enteraba de que había una
  venta cobrada de verdad sin resolver. `OrderService.confirmFromPayment`
  ahora atrapa esa falla adentro (en vez de dejarla subir sin más al
  webhook, que sólo la logueaba) y dos cosas quedan garantizadas incluso
  cuando `doConfirm` falla: el pago se marca igual como `APPROVED` (el
  cliente pagó de verdad, eso no se puede perder aunque el pedido no se
  pudo confirmar solo) y un `Order.paymentIssueNote` nuevo (aditivo, migró
  solo) queda con el detalle del problema. Visible en dos lugares del
  panel: un ⚠️ junto al código en el listado de "Pedidos", y un banner
  ámbar prominente arriba del todo en el detalle del pedido
  (`admin-order-detail`) con el texto completo. El webhook sigue teniendo
  su propio try/catch alrededor de la llamada — ahora es de verdad un
  catch-all para errores inesperados (de red, de la API de Mercado Pago),
  no para el caso ya cubierto de sin-stock.
  **Verificado con un test automatizado nuevo**
  (`OrderMercadoPagoConfirmTest`, primera cobertura automatizada de algo
  de Mercado Pago en el proyecto) que llama a `confirmFromPayment`
  directo — sin pasar por el webhook real, que necesitaría credenciales
  de Mercado Pago — con dos casos: sin stock suficiente (el pago queda
  `APPROVED`, el pedido sigue `PENDIENTE`, el stock no se toca, y
  `paymentIssueNote` tiene el mensaje) y con stock suficiente (confirma
  normal, `paymentIssueNote` queda `null`). Los 2 tests nuevos pasan y el
  resto de la suite (existente) sigue en verde. También verificado en el
  navegador pisando un pedido de prueba real por SQL (mismo mecanismo que
  ya se usó para `paymentStatus`) y revirtiéndolo al terminar: se ve el
  ⚠️ en el listado y el banner completo en el detalle.
- **2026-09-16**: recuperación de contraseña, de "manda una contraseña
  nueva por mail" a "manda un link de un solo uso" — primer ítem de una
  ronda de mejoras propuestas proactivamente (deuda técnica/seguridad +
  UX) tras cerrar Fase 12/13, priorizado por el usuario como el más
  urgente ("dinero real entrando por Mercado Pago"). El mecanismo viejo
  (`AuthService.forgotPassword` generaba una contraseña al azar y la
  mandaba directo por mail) tenía dos problemas: viajaba en texto plano,
  y — más grave — como el DNI no es secreto, cualquiera que lo supiera
  podía invalidar la contraseña real de otro admin en cualquier momento
  con sólo pedir la recuperación, sin necesitar leer el mail ajeno para
  causar el daño (DoS de cuenta). Ahora `forgotPassword` genera un token
  de 32 bytes al azar, guarda sólo su hash SHA-256 en `AdminUser`
  (`resetTokenHash`/`resetTokenExpiresAt`, aditivo, migró solo) y manda
  un link a `/admin/restablecer-clave?token=...` que vence a la hora y
  sirve una sola vez — pedir la recuperación ya no toca la cuenta hasta
  que alguien con acceso real al mail abre el link y confirma una
  contraseña nueva (`AuthService.resetPassword`, endpoint nuevo `POST
  /api/auth/reset-password`, público). Pantalla nueva
  `admin-reset-password` (mismo estilo que `admin-recover`, que ya
  existía para pedir el link). **Verificado de punta a punta**: probado
  el ciclo completo contra la cuenta real del superadmin (token inyectado
  a mano por SQL para poder probarlo sin acceso al buzón real, siempre
  restaurando `augusto123` al terminar cada prueba) — token válido
  cambia la contraseña y permite loguear con la nueva, el mismo token no
  se puede reusar, un token vencido se rechaza con mensaje claro, y el
  flujo real de `POST /api/auth/forgot-password` genera el token/expiry
  en UTC correctamente (se detectó y evitó un falso bug de zona horaria
  del entorno de prueba: `NOW()` de MySQL local es hora Argentina, no
  UTC — el código de la app usa `Instant.now()` de Java, que sí es UTC
  siempre, así que no le pasa lo mismo). Probado también en la UI real
  del navegador (no sólo por `curl`): pantalla sin token, con token
  inválido, y el formulario completo cambiando la contraseña de verdad.
- **2026-09-16**: bug reportado por el usuario, en dos vueltas — en "Ver en
  grande" de Apariencia y, más importante (lo que realmente había notado el
  usuario), en el asistente **"Crear tienda"**: el pie de página de la vista
  previa aparecía siempre con un marrón fijo, sin importar el rubro
  elegido ni el color de marca, en vez de la paleta que le correspondería
  a esa tienda en la página real.
  - **Primer intento (insuficiente):** `FooterComponent.footerBg()`
    trataba cualquier valor del input `footerColorOverride` (incluido
    `null` explícito, que es como arranca `draftFooterColor()` mientras
    nadie eligió un color de pie de página propio) como "el color a
    usar" en vez de "sin elegir todavía" — eso dejaba el pie de página
    sin ningún color (transparente, texto blanco invisible). El primer
    fix lo tapó con un fallback fijo (`#7c2d12`), lo cual arregló que se
    viera *algo* pero rompió la premisa real: todas las tiendas nuevas,
    sin importar el rubro, mostraban el mismo marrón — el usuario lo
    marcó explícitamente ("cuando pongo crear una tienda... todas me
    aparecen con el footer marrón cuando debería ser por defecto como
    es la página en realidad").
  - **Causa raíz real:** el pie de página, desde su versión original (Fase
    10, antes de romperse), no tiene un color fijo — usa la clase
    Tailwind `bg-brand-700`, que lee la variable de tema `--color-brand-700`
    (redefinida por `[data-theme]` según el rubro en el sitio real — ver
    Fase 9 — y por `previewVars()`/`generateBrandRamp()` según el color de
    marca elegido en las vistas previas de Apariencia y del asistente). En
    una sesión anterior (Fase 12) alguien sacó esa clase del `<footer>` y
    forzó siempre un `[style.background-color]` con `!important`, lo que
    de entrada desconectó el pie de página del sistema de temas — el
    fallback `#7c2d12` de mi primer intento heredó ese error en vez de
    corregirlo.
  - **Fix final:** restaurada la clase `bg-brand-700` en
    `footer.component.html` (sacado el `!important`); `footerBg()` en
    `footer.component.ts` ahora devuelve `null` (sin pisar nada) cuando no
    hay color guardado ni override elegido — igual que `headerBg()` en
    `HeaderComponent` — dejando que la clase (y por lo tanto el tema del
    rubro / el color de marca de la preview) se aplique sola. Un solo
    cambio en `FooterComponent` corrige los 3 lugares que lo usan
    (Apariencia "ver en grande" y las dos vistas del asistente
    "Crear tienda").
  - **Verificado en el navegador:** en el asistente, creando (sin llegar a
    confirmar) una tienda de rubro Ferretería sin tocar ningún color, el
    pie de página de la preview pasó de un marrón fijo y desentonado a
    seguir el mismo naranja que el resto de la vista previa (coherente,
    ya que el asistente no ata el rubro a `[data-theme]` en la preview,
    sólo al color de marca — eso es una limitación previa y separada, no
    parte de este bug). En el sitio real se confirmaron los dos extremos:
    la tienda piloto (`Estilos Pequeños`) sigue con su footer naranja de
    siempre, sin regresión, y "El Yunque" (rubro Ferretería, con su propio
    `data-theme="ferreteria"`) muestra el pie de página en el marrón-madera
    propio de ese tema — ya no el marrón genérico fijo.
- **2026-09-16**: Fase 12 — 3 bugs de las previews del asistente/Apariencia
  (miniaturas de diseño todas con el mismo color, encabezado/pie de página
  sin reaccionar al color y el pie de página ni aparecía, nombre/logo de
  tienda nueva mostrando los de la piloto) encontrados por el usuario
  probando la Fase 10, todos arreglados y verificados en el navegador.
  Después, primer módulo real gateado por `Plan.enabledModules` (Fase 5,
  hasta ahora sin usar): "Publicar en redes" — botón en el
  formulario/listado de productos que comparte foto + texto (título,
  descripción o texto libre, con preview en vivo) vía la Web Share API
  nativa del celular, sin API de Meta ni credenciales guardadas. Corregido
  en la misma sesión un bug real de pérdida de "activación de usuario" por
  un `await` de más antes de `navigator.share()`, y un bug de backfill que
  dejaba el módulo nuevo sin activar en el plan ya sembrado. Mercado Pago
  (checkout con pago online, pedido por el usuario en la misma sesión)
  discutido pero explícitamente diferido a su propia sesión de
  planificación — ver detalle completo en la sección Fase 12.
- **2026-09-16**: Fase 11 (nueva) — pausar/reanudar y eliminar tiendas,
  ambos a pedido del usuario. Pausar: `Tenant.active` (ya existía, sin
  usar) ahora se enforce de verdad en `TenantResolutionFilter` (503 en
  cualquier ruta que no sea `/api/admin/**` o `/api/auth/**`) — el panel
  de esa tienda sigue accesible siempre para poder reanudarla. Eliminar:
  `TenantDeletionService` nuevo, borra las 14 tablas propias del tenant en
  el orden correcto (AdminUser antes que Role, Order/Exchange/ParamGroup
  entidad-por-entidad para respetar sus cascadas) y por último la fila de
  `Tenant`, todo en una transacción; confirmación elegida por el usuario
  (escribir el slug exacto, validado también en el backend, no sólo en el
  frontend). Limitación conocida y documentada: no borra nada de
  Cloudinary (preset unsigned, sin API key/secret configurada) — quedan
  fotos huérfanas en la nube. Verificado de punta a punta con un tenant
  descartable creado y borrado en el navegador; el resto de las tiendas
  (piloto incluida) no se tocó. Ver detalle completo en la sección Fase 11.
- **2026-09-16**: Fase 10 ampliada, Pasos 7-11 — cierre del set de layouts
  y el asistente "Crear tienda", todo a pedido del usuario en la misma
  sesión larga de trabajo:
  - Paso 7: descartados los 3 layouts que el usuario rechazó dos veces
    (`editorial`/`marketplace`/`vidriera`, hechos de memoria vaga y
    "iguales a Clásico"); reemplazados por 5 layouts reales inspirados en
    6 temas de WordPress GPL que bajó el usuario (`minimal`, `boutique`,
    `curva`, `grid`, `mercado` — este último consolida 3 temas casi
    idénticos entre sí), set final de 6 layouts contando `classic`.
    Agregado un logo (`LogoComponent` nuevo) en cada uno de los 5
    layouts que no lo tenían, en un lugar propio de cada composición.
  - Paso 8: `logoShape` (circle/square/rectangle) — elección explícita
    del usuario al subir el logo, no detección automática (se descartó
    por poco confiable). Selector en "Identidad y contacto" y en el
    asistente.
  - Paso 9: 4 colores independientes del color de marca (encabezado, pie
    de página, títulos/nombre, fondo de página) — mecanismo vía inputs
    `[style.x]`/overrides de componente (NO `var(--x, revert)`: ese
    fallback es inválido según la spec de CSS Custom Properties, se
    evaluó y se descartó). UI duplicada (no componente compartido) en
    "Apariencia" y en el paso Color del asistente.
  - Paso 10: sugerencia de color de marca extraído del logo
    (`core/utils/logo-color.ts`, 100% client-side con `<canvas>`) —
    motivó mover el paso Logo del asistente antes que el paso Color.
  - Paso 11: asistente reordenado a 7 pasos (Diseño→Logo→Color→
    Identidad→Preview→Confirmar→Listo); 2 bugs corregidos (el logo no se
    veía en la vista previa — faltaba un input `logoOverride`; el paso de
    Previsualización no tenía botón "Atrás"); y un cambio de
    comportamiento a pedido del usuario: el logo ya NO se sube a
    Cloudinary al elegirlo, queda en memoria (data URL) hasta que se
    confirma "Crear tienda" — evita imágenes húerfanas si se cancela.
  Todo verificado en el navegador esta sesión, sin tocar datos reales de
  ninguna tienda existente. Ver detalle completo (incluyendo qué NO se
  hizo) en la sección Fase 10.
- **2026-09-16**: Fase 10, Pasos 4-6 — el superadmin ya puede configurar
  cualquier tienda (no sólo mirarla): endpoint de apariencia
  (`layout`+`brandColor`), pantalla "Apariencia" con miniaturas reales
  (`CatalogPageComponent` con un `layoutOverride` nuevo) y botón
  "Configurar esta tienda" en el listado de tiendas, que reutiliza el
  demo-switch para llegar a `/admin/config` de cualquier tenant. La causa
  del reclamo original del usuario ("no puedo poner nombre, redes,
  contacto, logo, nada" al crear una tienda) no era el formulario de
  creación sino que ningún tenant nuevo tenía un `AdminUser` propio —
  se resolvió reutilizando el mecanismo de superadmin cross-tenant que ya
  existía, en vez de crear usuarios admin por tenant. Verificado en el
  navegador contra `el-yunque`, revertido después.
- **2026-09-15**: arrancada la Fase 10 (layout + color de marca
  configurables, independientes entre sí) — Pasos 1 a 3 hechos y
  verificados en el navegador: campos `SiteSettings.layout`/`brandColor`
  (backend, 100% aditivo), `SettingsService` aplicando `data-layout` +
  una rampa de color derivada en runtime (`core/utils/color-ramp.ts`), y
  un primer segundo layout real (`"editorial"`) como una rama de
  template dentro de `CatalogPageComponent` en vez de un componente
  duplicado. Confirmado contra `el-yunque` (revertido después) que la
  tienda piloto no cambia y que layout/color son independientes. Falta
  el editor real (Paso 4) y los layouts 3-5 (Paso 5) — ver detalle en la
  sección Fase 10.
- **2026-09-15**: logo, tagline y carrusel propios por tenant — a pedido
  del usuario tras notar que "El Yunque" se veía con el logo y el texto
  de Estilos Pequeños. `RubroImages` (nueva clase) genera logo/fotos de
  producto/banners de carrusel como SVG data URI por rubro, sin depender
  de subir nada a Cloudinary. `TenantProvisioningService` ahora siembra
  logo + 2 fotos de carrusel al crear una tienda; `DataSeeder` completa
  (en cada arranque) las que ya existían sin eso. Verificado en el
  navegador: "El Yunque" y "El Cigüeñal" ya muestran su propio logo y
  carrusel temático; la tienda piloto sumó carrusel sin perder su logo
  real. Ver detalle en la sección Fase 9.
- **2026-09-15**: cerrada la Fase 9 con los themes reales de ferretería y
  repuestos (`[data-theme="ferreteria"]`/`"repuestos"` en `styles.css`,
  fuentes Oswald/Teko sumadas en `index.html`) — ver detalle en la
  sección Fase 9. Verificado en el navegador contra las 2 tiendas creadas
  antes (`el-yunque`, `el-ciguenal`): cambian paleta y tipografía en todo
  el sitio público sin afectar a la tienda piloto.
- **2026-09-15 (incidente, no relacionado a código)**: el reset de MySQL
  de esta misma fase (necesario para la columna `rubro` NOT NULL) borró
  3 pedidos de prueba y 3 reglas de descuento que sólo existían en una
  base `estilos_pequenos` vieja (previa al rename a `saasweb`, nunca
  borrada). Se recuperaron leyendo el binlog de MySQL (retención 30 días,
  cubría desde la creación original de la base) y migrando esas filas a
  `saasweb` con `INSERT ... SELECT` cross-database, a pedido explícito
  del usuario tras notar la diferencia. Se buscaron también URLs de fotos
  de Cloudinary para el carrusel (`hero_slide`) pedidas por el usuario:
  no hay rastro en ningún binlog — nunca se guardó una fila ahí, así que
  no es recuperable desde la base (si las fotos siguen en la cuenta de
  Cloudinary, hay que volver a cargarlas a mano desde `/admin/carrusel`).
  Sin cambios de código — sólo datos.
- **2026-09-15**: implementado el backend de la Fase 9 de verdad (no sólo
  plantillas): `TenantProvisioningService` + `TenantController`
  (`/api/admin/tenants`, sólo superadmin) para crear tenants nuevos con
  seed de ejemplo por rubro, y el selector de tienda modo demo
  (`X-Demo-Tenant` header en `TenantResolutionFilter`). Motivado por el
  pedido real del usuario: un asistente "Crear tienda" en el panel para
  mostrar en su clase, sin necesitar subdominios/DNS reales todavía.
  Verificado de punta a punta contra MySQL real (reset + reseed): creados
  `el-yunque` (FERRETERIA) y `el-ciguenal` (REPUESTOS), cada uno devuelve
  settings/catálogo aislados vía el header demo, y la tienda piloto no
  tuvo ninguna regresión. También se construyó el lado frontend (mismo
  día, repo `frontend-ecommerce---ruth`): asistente "Crear tienda"
  (`/admin/superadmin/tiendas`), `DemoTenantService` +
  `demoTenantInterceptor`, y el banner de tienda demo — probado en el
  navegador creando una tercera tienda desde la UI y confirmando que el
  catálogo/filtros/precios del rubro elegido se ven correctos. Pendiente:
  el CSS real de los themes `ferreteria`/`repuestos` (ver detalle en
  sección Fase 9) — hoy las tiendas nuevas se ven con la paleta de
  Estilos Pequeños.
- **2026-09-15**: agregado modo oscuro real (Fase 6, segundo paso), a
  pedido del usuario, como eje aparte del theme de marca (preferencia del
  visitante en `localStorage`, no del tenant). Alcance acotado a
  header + home (logo/intro + heading de "Lo más vendido") — el resto del
  sitio no reacciona todavía. Verificado en el navegador en los dos
  sentidos del toggle.
- **2026-09-15**: ampliada la Fase 7 — segundo bloque (`FEATURED_PRODUCTS`,
  envuelve "Lo más vendido") y pantalla de admin nueva (`/admin/inicio`)
  para mostrar/ocultar bloques sin llamar la API a mano. Verificado de
  punta a punta en el navegador con ambos servidores contra MySQL real
  (reseteada para sembrar los bloques nuevos).
- **2026-09-15**: hecha la base de la Fase 6 (themes): `SiteSettings.theme`
  + `data-theme` aplicado en `<html>` desde el frontend. Verificado en el
  navegador (no sólo asumido de la doc de Tailwind) que sobreescribir una
  variable CSS de Tailwind v4 en runtime cambia el color de los elementos
  al instante — confirma que el mecanismo para ofrecer temas reales
  funciona. No se inventó ningún theme nuevo (sería una decisión de diseño
  que nadie pidió) — sigue habiendo un solo theme, `"default"`.
- **2026-09-15**: hecho el primer bloque de la Fase 7 (personalizador
  visual): entidad `PageBlock` + endpoints, bloque "HERO" mostrable/
  ocultable, y el frontend (`frontend-ecommerce---ruth`, repo separado)
  consumiéndolo — probado de punta a punta en el navegador con ambos
  servidores contra MySQL real. Se respetó el `CLAUDE.md` del frontend
  (cambios chicos e iterativos): sólo un bloque, sin pantalla de admin ni
  reordenamiento todavía. Fase 6 (themes) sigue sin arrancar.
- **2026-09-15**: definidos los rubros concretos de la Fase 9 (ferretería y
  repuestos de vehículos) y confirmado con el usuario que no necesitan
  stock por variante — esto evita tener que generalizar `SizeScale`/
  `SizeStock`. Documentadas las plantillas de `ParamGroup` para ambos
  rubros (sección Fase 9), sin cargarlas en ningún tenant (no existe
  todavía un segundo tenant real). También se relevó el proyecto frontend
  Angular (`frontend-ecommerce---ruth`) para preparar las Fases 6/7 — tiene
  su propio `CLAUDE.md` que pide cambios chicos e iterativos, así que esas
  fases se van a encarar de a un paso, empezando por el backend de
  `PAGE_BLOCK` + el bloque "hero" en el frontend (Fase 7), no las dos
  fases completas de una.
- **2026-09-15**: creado el documento. Análisis inicial del modelo hecho.
  Decisión: la tienda actual es una prueba real de uso único; multi-tenancy
  y todo lo relacionado queda diferido hasta validarla.
- **2026-09-15**: confirmado el nombre `saasweb`. Ejecutado el rename de
  paquete Java (`com.estilospequenos` → `com.saasweb`) y `pom.xml`, docs
  actualizados. DB y datos de marca (`estilos_pequenos`, dominio de mail,
  Instagram, Cloudinary) quedaron sin tocar a propósito. Compila y los 28
  tests pasan.
- **2026-09-15**: cambio de estrategia — el código se va a ir adaptando a
  multi-tenant desde ahora (no recién cuando exista un segundo cliente),
  para no reescribir trabajo. Implementada la Fase 3 (infraestructura de
  tenant: entidad `Tenant`, `TenantService`, `TenantContext`,
  `TenantResolutionFilter`) sin tocar ninguna entidad de negocio — aditivo,
  compila, 28 tests en verde.
- **2026-09-15**: confirmado el supuesto (1 admin = 1 tenant, superadmin
  fuera del esquema de tenant) y ejecutada la Fase 4 completa: `tenant_id`
  en 13 entidades, filtrado explícito en todos los repositories/services,
  SiteSettings/MarketingConfig pasaron de singleton a "una fila por
  tenant", uniques compuestos donde correspondía. Compila y los 28 tests
  pasan. Como no había datos importantes en la MySQL local, se decidió
  además renombrar la base `estilos_pequenos` → `saasweb` (en vez de migrar
  los datos existentes) — actualizado `application.yml`, `database/*.sql` y
  `PROYECTO.md`/`README.md`. La base vieja queda huérfana en MySQL, sin
  borrar. `database/*.sql` todavía no refleja el ESQUEMA con `tenant_id`
  (sólo el nombre de la base) — pendiente. Próximo paso: retomar Fase 2
  (repackage por feature) o seguir con las fases de negocio (5+) cuando
  corresponda.
- **2026-09-15**: ejecutada la Fase 2 (repackage a package-by-feature),
  confirmando antes con el usuario que revertía la organización
  package-by-layer documentada como "pedido del cliente". 112 archivos
  movidos, imports arreglados en varias pasadas (directos, miembros
  anidados, referencias calificadas inline, y ~10 imports que quedaron
  implícitos por compartir paquete-por-capa antes). Compila y los 28 tests
  pasan. `PROYECTO.md`/`README.md` actualizados.
- **2026-09-15**: ejecutada la Fase 5 (solo infraestructura, confirmado con
  el usuario). Entidad `Plan` + `Tenant.planId` (NOT NULL) + límites de
  productos/usuarios realmente enforced en `ProductService`/
  `AdminUserService`. Plan por defecto sembrado sin límites reales.
  `enabledModules` existe pero no está gateado todavía (no hay un segundo
  módulo que activar/desactivar). Compila y los 28 tests pasan. **Nota:**
  como `Tenant` ya tiene una fila en la MySQL local (de la Fase 4), agregar
  `plan_id` NOT NULL va a repetir el mismo problema de arranque que ya
  vimos — mismo criterio: reset de la base local (`saasweb`), no hay datos
  importantes cargados.
