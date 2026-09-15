# Plan de evolución a SaaS — Roadmap

> Documento vivo. Registra QUÉ se decidió, QUÉ se hizo y QUÉ queda pendiente
> del plan para evolucionar este e-commerce hacia una plataforma SaaS
> multi-tenant. Se actualiza cada vez que se avanza una fase (no día a día:
> para eso está el Historial de `PROYECTO.md`).

Última actualización: 2026-09-15.

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

---

## 5. Historial

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
