package com.saasweb.core.page;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PageBlockService {

    public static final String HOME = "HOME";
    public static final String HERO = "HERO";

    private final PageBlockRepository repo;

    public PageBlockService(PageBlockRepository repo) {
        this.repo = repo;
    }

    /** Bloques visibles de una página, en orden — lo que consume el sitio público. */
    @Transactional(readOnly = true)
    public List<PageBlock> findVisible(String pageType) {
        return repo.findByTenantIdAndPageTypeAndVisibleTrueOrderByPositionAsc(TenantContext.getTenantId(), pageType);
    }

    /** Todos los bloques de una página (visibles u ocultos) — para el panel. */
    @Transactional(readOnly = true)
    public List<PageBlock> findAll(String pageType) {
        return repo.findByTenantIdAndPageTypeOrderByPositionAsc(TenantContext.getTenantId(), pageType);
    }

    public PageBlock setVisible(String id, boolean visible) {
        PageBlock b = get(id);
        b.setVisible(visible);
        return repo.save(b);
    }

    private PageBlock get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Bloque", id));
    }

    /** Crea el bloque HERO de la home si todavía no existe. Se llama desde el DataSeeder. */
    public void ensureHeroBlock(String tenantId) {
        if (!repo.findByTenantIdAndPageTypeOrderByPositionAsc(tenantId, HOME).isEmpty()) return;
        PageBlock hero = new PageBlock();
        hero.setId(java.util.UUID.randomUUID().toString());
        hero.setTenantId(tenantId);
        hero.setPageType(HOME);
        hero.setBlockType(HERO);
        hero.setPosition(0);
        hero.setVisible(true);
        repo.save(hero);
    }
}
