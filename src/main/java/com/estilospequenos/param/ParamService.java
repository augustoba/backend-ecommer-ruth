package com.estilospequenos.param;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.common.Slugs;
import com.estilospequenos.param.ParamDtos.GroupRequest;
import com.estilospequenos.param.ParamDtos.OptionRequest;
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
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public ParamGroup get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Parametría", id));
    }

    public ParamGroup create(GroupRequest req) {
        ParamGroup g = new ParamGroup();
        g.setId("grp-" + Slugs.slug(req.name()) + "-" + Slugs.shortRandom());
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
