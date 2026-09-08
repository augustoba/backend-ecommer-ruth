package com.estilospequenos.heroslide;

import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.heroslide.HeroSlideDtos.SlideRequest;
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
        return repo.findAllByOrderByPositionAsc();
    }

    public HeroSlide create(SlideRequest req) {
        HeroSlide s = new HeroSlide();
        s.setId(UUID.randomUUID().toString());
        s.setImageUrl(req.imageUrl().trim());
        s.setAlt(alt(req.alt()));
        s.setPosition(repo.count() == 0 ? 0 : (int) repo.count());
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
        List<HeroSlide> all = repo.findAllByOrderByPositionAsc();
        for (HeroSlide s : all) {
            int idx = ids.indexOf(s.getId());
            s.setPosition(idx >= 0 ? idx : ids.size());
        }
        repo.saveAll(all);
        return repo.findAllByOrderByPositionAsc();
    }

    private HeroSlide get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Foto del carrusel", id));
    }

    private static String alt(String alt) {
        return (alt == null || alt.isBlank()) ? "Foto de la tienda" : alt.trim();
    }
}
