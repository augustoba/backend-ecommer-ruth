package com.saasweb.core.plan;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Garantiza que existan los planes base (ver {@link #ensureDefault()} /
 * {@link #ensurePosPlan()}) y resuelve el plan del tenant actual. Desde la
 * Fase 17 hay dos: "Ecommerce" (fallback histórico) y "Punto de venta" —
 * cada uno con su propio set de módulos, editable desde
 * `/admin/superadmin/planes`. Cargar límites/precios de negocio reales
 * sigue siendo dato (UPDATE desde ese panel), no código.
 */
@Service
@Transactional
public class PlanService {

    public static final String DEFAULT_SLUG = "default";

    private final PlanRepository repo;
    private final TenantRepository tenants;

    public PlanService(PlanRepository repo, TenantRepository tenants) {
        this.repo = repo;
        this.tenants = tenants;
    }

    public static final String POS_SLUG = "punto-de-venta";

    /**
     * Crea el plan "Ecommerce" (fallback histórico, ver {@code DEFAULT_SLUG})
     * si todavía no existe. Idempotente — a diferencia de antes, YA NO
     * fuerza módulos en cada arranque a un plan que ya existe: desde que
     * hay más de un plan (ver {@link #ensurePosPlan()}), cada uno tiene
     * deliberadamente un set de módulos distinto (Fase 17) y un backfill
     * "agregar todo lo nuevo" pisaría esa diferenciación en el próximo
     * restart. Un módulo nuevo que sume `Modules` de acá en más se agrega
     * a mano desde `/admin/superadmin/planes` al plan que corresponda.
     */
    public Plan ensureDefault() {
        return repo.findBySlug(DEFAULT_SLUG).orElseGet(() -> {
            Plan created = new Plan();
            created.setId(UUID.randomUUID().toString());
            created.setSlug(DEFAULT_SLUG);
            created.setName("Ecommerce");
            created.setBusinessModel(BusinessModel.ECOMMERCE);
            created.setMaxProducts(null); // sin límite: todavía no hay planes de negocio definidos
            created.setMaxAdminUsers(null);
            created.setEnabledModules(new LinkedHashSet<>(
                    Set.of("ropa", Modules.SOCIAL_SHARE, Modules.MERCADOPAGO, Modules.ECOMMERCE_SITE,
                            Modules.ARCA_INVOICING)));
            created.setShowPlatformBranding(false);
            return repo.save(created);
        });
    }

    /**
     * Plan "Punto de venta" (Fase 17) — para tenants sin vidriera online
     * (kiosco, casa de repuestos que sólo vende presencial). Se le puede
     * sumar/sacar módulos después desde el panel como a cualquier otro plan.
     */
    public Plan ensurePosPlan() {
        return repo.findBySlug(POS_SLUG).orElseGet(() -> {
            Plan created = new Plan();
            created.setId(UUID.randomUUID().toString());
            created.setSlug(POS_SLUG);
            created.setName("Punto de venta");
            created.setBusinessModel(BusinessModel.POS);
            created.setMaxProducts(null);
            created.setMaxAdminUsers(null);
            created.setEnabledModules(new LinkedHashSet<>(Set.of(Modules.POS, Modules.ARCA_INVOICING)));
            created.setShowPlatformBranding(false);
            return repo.save(created);
        });
    }

    /**
     * Completa `businessModel` de los dos planes que siembra esta clase
     * para el caso de filas que ya existían de antes de la Fase 17 —
     * idempotente (siempre el mismo valor para ese slug), corre en cada
     * arranque. Un plan con slug desconocido (creado a mano por SQL, fuera
     * de estos dos) queda con `businessModel=null`, tratado como ECOMMERCE
     * por {@link Plan#getBusinessModel()} — la opción menos restrictiva.
     */
    public void backfillBusinessModel() {
        repo.findBySlug(DEFAULT_SLUG).ifPresent(p -> {
            p.setBusinessModel(BusinessModel.ECOMMERCE);
            repo.save(p);
        });
        repo.findBySlug(POS_SLUG).ifPresent(p -> {
            p.setBusinessModel(BusinessModel.POS);
            repo.save(p);
        });
    }

    @Transactional(readOnly = true)
    public boolean exists(String planId) {
        return planId != null && repo.existsById(planId);
    }

    /** Plan del tenant actual (vía TenantContext). null si el tenant no tiene uno asignado. */
    @Transactional(readOnly = true)
    public Plan getCurrent() {
        return getForTenant(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Plan getForTenant(String tenantId) {
        if (tenantId == null) return null;
        String planId = tenants.findById(tenantId).map(Tenant::getPlanId).orElse(null);
        return planId != null ? repo.findById(planId).orElse(null) : null;
    }

    /** Todos los planes de la plataforma (ver PlanController). */
    @Transactional(readOnly = true)
    public List<Plan> findAll() {
        return repo.findAll();
    }

    /** Cuántas tiendas usan un plan — contexto para el panel antes de cambiarle límites/módulos. */
    @Transactional(readOnly = true)
    public long countTenantsUsing(String planId) {
        return tenants.countByPlanId(planId);
    }

    /**
     * Edita límites/módulos/branding de un plan ya existente — el slug y el
     * `businessModel` no se tocan (identificadores estables; reclasificar
     * un plan de Ecommerce a POS con tiendas ya usándolo sería disruptivo).
     * `enabledModules` se filtra contra {@link Modules#compatibleWith} —
     * defensa en profundidad: el editor del panel ya sólo ofrece tildar
     * los compatibles, esto es por si alguien pega directo a la API.
     * No hay todavía un ABM para crear planes nuevos desde el panel (sigue
     * siendo un INSERT a mano, ver PLAN_SAAS.md) — sólo existen dos hoy.
     */
    public Plan update(String id, String name, Integer maxProducts, Integer maxAdminUsers,
                        Set<String> enabledModules, boolean showPlatformBranding) {
        Plan plan = repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Plan", id));
        Set<String> compatible = Modules.compatibleWith(plan.getBusinessModel());
        Set<String> filtered = new LinkedHashSet<>(enabledModules);
        filtered.retainAll(compatible);
        plan.setName(name);
        plan.setMaxProducts(maxProducts);
        plan.setMaxAdminUsers(maxAdminUsers);
        plan.setEnabledModules(filtered);
        plan.setShowPlatformBranding(showPlatformBranding);
        return repo.save(plan);
    }
}
