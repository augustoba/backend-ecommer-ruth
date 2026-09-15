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

### Fase 6 — Themes en Angular ⏸️

### Fase 7 — Personalizador visual (bloques de página) ⏸️

### Fase 8 — Dominios propios, SSL, deployment con Docker + reverse proxy ⏸️

### Fase 9 — Agregar el segundo rubro (ej. ferretería) reutilizando la plataforma ⏸️

---

## 5. Historial

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
