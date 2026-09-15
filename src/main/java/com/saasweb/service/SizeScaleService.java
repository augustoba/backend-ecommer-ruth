package com.saasweb.service;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.Slugs;
import com.saasweb.common.TenantContext;
import com.saasweb.dto.SizeScaleDtos.ScaleRequest;
import com.saasweb.model.SizeScale;
import com.saasweb.repository.SizeScaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional
public class SizeScaleService {

    private final SizeScaleRepository repo;

    public SizeScaleService(SizeScaleRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<SizeScale> findAll() {
        return repo.findByTenantId(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public SizeScale get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Escala de talle", id));
    }

    public SizeScale create(ScaleRequest req) {
        SizeScale s = new SizeScale();
        s.setId("escala-" + Slugs.slug(req.name()) + "-" + Slugs.shortRandom());
        s.setTenantId(TenantContext.getTenantId());
        s.setName(req.name().trim());
        s.setSystem(false);
        s.setValues(dedupe(req.values()));
        return repo.save(s);
    }

    public SizeScale updateName(String id, ScaleRequest req) {
        SizeScale s = get(id);
        s.setName(req.name().trim());
        if (req.values() != null) s.setValues(dedupe(req.values()));
        return repo.save(s);
    }

    public SizeScale replaceValues(String id, List<String> values) {
        SizeScale s = get(id);
        s.setValues(dedupe(values));
        return repo.save(s);
    }

    public void delete(String id) {
        SizeScale s = get(id);
        if (s.isSystem()) {
            throw new BadRequestException("No se puede eliminar una escala de sistema.");
        }
        repo.delete(s);
    }

    private static List<String> dedupe(List<String> values) {
        if (values == null) return new ArrayList<>();
        var seen = new LinkedHashSet<String>();
        for (String v : values) {
            if (v != null && !v.isBlank()) seen.add(v.trim());
        }
        return new ArrayList<>(seen);
    }
}
