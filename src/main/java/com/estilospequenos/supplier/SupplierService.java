package com.estilospequenos.supplier;

import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.supplier.SupplierDtos.SupplierRequest;
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
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Proveedor", id));
    }

    public Supplier create(SupplierRequest req) {
        Supplier s = new Supplier();
        s.setId(UUID.randomUUID().toString());
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
