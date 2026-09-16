package com.saasweb.core.admin;

import com.saasweb.core.admin.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByTenantIdAndNameIgnoreCase(String tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCase(String tenantId, String name);

    /** El único rol de sistema (Superadmin) — compartido, no pertenece a ningún tenant. */
    Optional<Role> findFirstBySystemTrue();

    /** Roles del tenant + el rol de sistema (aparece en el listado, aunque sólo lo pueda asignar un superadmin). */
    @Query("select r from Role r where r.tenantId = :tenantId or r.system = true order by r.system desc, r.name asc")
    List<Role> findAllForTenant(@Param("tenantId") String tenantId);

    @Query("select r from Role r where r.id = :id and (r.tenantId = :tenantId or r.system = true)")
    Optional<Role> findByIdForTenant(@Param("id") String id, @Param("tenantId") String tenantId);

    /**
     * Borra los roles propios del tenant — ver TenantDeletionService. Nunca
     * toca el rol de sistema (`system = true`, `tenantId` null): el filtro
     * `tenantId = :tenantId` ya lo excluye solo. Correr DESPUÉS de borrar
     * los AdminUser del tenant (ver esa nota en AdminUserRepository).
     */
    void deleteAllByTenantId(String tenantId);
}
