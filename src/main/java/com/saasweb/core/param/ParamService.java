package com.saasweb.core.param;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.Slugs;
import com.saasweb.common.TenantContext;
import com.saasweb.core.param.ParamDtos.GroupRequest;
import com.saasweb.core.param.ParamDtos.OptionRequest;
import com.saasweb.core.param.ParamGroup;
import com.saasweb.core.param.ParamOption;
import com.saasweb.core.param.ParamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ParamService {

    private final ParamRepository repo;

    public ParamService(ParamRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ParamGroup> findAll() {
        return repo.findByTenantId(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public ParamGroup get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Parametría", id));
    }

    public ParamGroup create(GroupRequest req) {
        ParamGroup g = new ParamGroup();
        g.setId("grp-" + Slugs.slug(req.name()) + "-" + Slugs.shortRandom());
        g.setTenantId(TenantContext.getTenantId());
        g.setName(req.name().trim());
        g.setMultiple(req.multiple());
        g.setShowInCatalog(req.showInCatalog() == null || req.showInCatalog());
        g.setSystem(false);
        return repo.save(g);
    }

    public ParamGroup update(String id, GroupRequest req) {
        ParamGroup g = get(id);
        g.setName(req.name().trim());
        g.setMultiple(req.multiple());
        if (req.showInCatalog() != null) g.setShowInCatalog(req.showInCatalog());
        return repo.save(g);
    }

    public void delete(String id) {
        ParamGroup g = get(id);
        if (g.isSystem()) {
            throw new BadRequestException("No se puede eliminar una parametría de sistema.");
        }
        repo.delete(g);
    }

    public ParamGroup addOption(String groupId, OptionRequest req) {
        ParamGroup g = get(groupId);
        ParamOption o = new ParamOption();
        o.setId(prefixOf(groupId) + "-" + Slugs.slug(req.label()) + "-" + Slugs.shortRandom());
        o.setLabel(req.label().trim());
        g.addOption(o);
        return repo.save(g);
    }

    public ParamGroup updateOption(String groupId, String optionId, OptionRequest req) {
        ParamGroup g = get(groupId);
        ParamOption o = g.getOptions().stream()
                .filter(x -> x.getId().equals(optionId)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Opción", optionId));
        o.setLabel(req.label().trim());
        return repo.save(g);
    }

    public ParamGroup removeOption(String groupId, String optionId) {
        ParamGroup g = get(groupId);
        boolean removed = g.getOptions().removeIf(x -> x.getId().equals(optionId));
        if (!removed) throw ResourceNotFoundException.of("Opción", optionId);
        for (int i = 0; i < g.getOptions().size(); i++) g.getOptions().get(i).setPosition(i);
        return repo.save(g);
    }

    private static String prefixOf(String groupId) {
        return groupId.startsWith("grp-") ? groupId.substring(4) : groupId;
    }
}
