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
    public static final String FEATURED_PRODUCTS = "FEATURED_PRODUCTS";

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

    /** Crea los bloques por defecto de la home si todavía no existe ninguno. Se llama desde el DataSeeder. */
    public void ensureDefaultHomeBlocks(String tenantId) {
        if (!repo.findByTenantIdAndPageTypeOrderByPositionAsc(tenantId, HOME).isEmpty()) return;
        repo.save(block(tenantId, HERO, 0));
        repo.save(block(tenantId, FEATURED_PRODUCTS, 1));
    }

    private PageBlock block(String tenantId, String blockType, int position) {
        PageBlock b = new PageBlock();
        b.setId(java.util.UUID.randomUUID().toString());
        b.setTenantId(tenantId);
        b.setPageType(HOME);
        b.setBlockType(blockType);
        b.setPosition(position);
        b.setVisible(true);
        return b;
    }
}
