package com.saasweb.service;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.dto.HeroSlideDtos.SlideRequest;
import com.saasweb.model.HeroSlide;
import com.saasweb.repository.HeroSlideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HeroSlideService {

    private final HeroSlideRepository repo;

    public HeroSlideService(HeroSlideRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<HeroSlide> findAll() {
        return repo.findByTenantIdOrderByPositionAsc(TenantContext.getTenantId());
    }

    public HeroSlide create(SlideRequest req) {
        String tenantId = TenantContext.getTenantId();
        HeroSlide s = new HeroSlide();
        s.setId(UUID.randomUUID().toString());
        s.setTenantId(tenantId);
        s.setImageUrl(req.imageUrl().trim());
        s.setAlt(alt(req.alt()));
        s.setPosition((int) repo.countByTenantId(tenantId));
        return repo.save(s);
    }

    public HeroSlide update(String id, SlideRequest req) {
        HeroSlide s = get(id);
        s.setImageUrl(req.imageUrl().trim());
        s.setAlt(alt(req.alt()));
        return repo.save(s);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    public List<HeroSlide> reorder(List<String> ids) {
        String tenantId = TenantContext.getTenantId();
        List<HeroSlide> all = repo.findByTenantIdOrderByPositionAsc(tenantId);
        for (HeroSlide s : all) {
            int idx = ids.indexOf(s.getId());
            s.setPosition(idx >= 0 ? idx : ids.size());
        }
        repo.saveAll(all);
        return repo.findByTenantIdOrderByPositionAsc(tenantId);
    }

    private HeroSlide get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Foto del carrusel", id));
    }

    private static String alt(String alt) {
        return (alt == null || alt.isBlank()) ? "Foto de la tienda" : alt.trim();
    }
}
