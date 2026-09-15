package com.saasweb.core.supplier;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.supplier.SupplierDtos.SupplierRequest;
import com.saasweb.core.supplier.Supplier;
import com.saasweb.core.supplier.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repo;

    public SupplierService(SupplierRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return repo.findByTenantId(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Supplier get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", id));
    }

    public Supplier create(SupplierRequest req) {
        Supplier s = new Supplier();
        s.setId(UUID.randomUUID().toString());
        s.setTenantId(TenantContext.getTenantId());
        apply(s, req);
        return repo.save(s);
    }

    public Supplier update(String id, SupplierRequest req) {
        Supplier s = get(id);
        apply(s, req);
        return repo.save(s);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    private void apply(Supplier s, SupplierRequest req) {
        s.setName(req.name().trim());
        s.setPhone(blankToNull(req.phone()));
        s.setAddress(blankToNull(req.address()));
        s.setNotes(blankToNull(req.notes()));
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
