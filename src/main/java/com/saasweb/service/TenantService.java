package com.saasweb.service;

import com.saasweb.config.AppProperties;
import com.saasweb.model.Tenant;
import com.saasweb.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Mientras haya un único tenant por deploy (ver PLAN_SAAS.md), este service
 * sólo garantiza que esa fila exista (sembrada desde {@code app.tenant.*}) y
 * la resuelve para {@code TenantResolutionFilter}. El día que haya
 * resolución real por dominio, acá se suma `findByHost`/CRUD de tenants —
 * la fila que crea `ensureDefault()` sigue siendo la de esta tienda.
 */
@Service
@Transactional
public class TenantService {

    private final TenantRepository repo;
    private final AppProperties props;

    public TenantService(TenantRepository repo, AppProperties props) {
        this.repo = repo;
        this.props = props;
    }

    /** Crea el tenant único de este deploy si todavía no existe. Idempotente. */
    public Tenant ensureDefault() {
        String slug = props.getTenant().getSlug();
        return repo.findBySlug(slug).orElseGet(() -> {
            Tenant t = new Tenant();
            t.setId(UUID.randomUUID().toString());
            t.setSlug(slug);
            t.setName(props.getTenant().getName());
            return repo.save(t);
        });
    }

    /** Id del tenant que debe usar la request actual. Hoy: siempre el único activo. */
    @Transactional(readOnly = true)
    public String resolveCurrentTenantId() {
        return repo.findFirstByActiveTrueOrderByCreatedAtAsc().map(Tenant::getId).orElse(null);
    }
}
