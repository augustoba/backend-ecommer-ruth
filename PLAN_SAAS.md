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

### Fase 2 — Repackage por feature 🔜
Reorganizar `com.<nombre>.{controller,service,model,repository}` (hoy
package-by-layer) hacia algo tipo:
```
core/         -> Product (sin talle), Order, Discount, Coupon, AdminUser, Role, Permission...
modules/ropa/ -> SizeScale, SizeStock, integración con Product
platform/     -> PlatformMailSettings
```
Sin cambios de comportamiento ni de DB — solo mover clases y dejar la
frontera core/ropa visible. Bajo riesgo, tests en verde en cada paso.
- [ ] Ejecutar repackage

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

### Fase 4 — `tenant_id` en entidades CORE + aislamiento real ⏸️ (siguiente paso grande)
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

### Fase 5 — Planes, límites y módulos activables por tenant ⏸️

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
  compila, 28 tests en verde. Próximo paso: Fase 4 (tenant_id en entidades
  CORE), pendiente confirmar el supuesto de `AdminUser`/`superAdmin` antes
  de arrancar.
