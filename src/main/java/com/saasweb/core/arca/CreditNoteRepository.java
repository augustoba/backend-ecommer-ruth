package com.saasweb.core.arca;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditNoteRepository extends JpaRepository<CreditNote, String> {
    List<CreditNote> findByTenantIdAndOrderIdOrderByCreatedAtDesc(String tenantId, String orderId);

    /** Ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);
}
