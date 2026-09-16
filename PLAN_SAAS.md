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

## 5. Historial

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
