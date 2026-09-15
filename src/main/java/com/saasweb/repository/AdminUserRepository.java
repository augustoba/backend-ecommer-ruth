package com.saasweb.repository;

import com.saasweb.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

    Optional<AdminUser> findByIdAndTenantId(String id, String tenantId);

    /**
     * Resuelve un DNI contra el tenant actual: un admin de ESE tenant, o el
     * superadmin de la plataforma (tenantId null, ve/opera en cualquier
     * tenant). Es lo que usan login y el filtro de JWT.
     */
    @Query("select u from AdminUser u where u.dni = :dni and (u.tenantId = :tenantId or u.tenantId is null)")
    Optional<AdminUser> findByDniForTenant(@Param("dni") String dni, @Param("tenantId") String tenantId);

    Optional<AdminUser> findByDniAndTenantId(String dni, String tenantId);

    Optional<AdminUser> findByDniAndTenantIdIsNull(String dni);

    boolean existsByTenantIdAndDniIgnoreCase(String tenantId, String dni);

    boolean existsByTenantIdAndEmailIgnoreCase(String tenantId, String email);

    List<AdminUser> findByTenantIdOrderByCreatedAtAsc(String tenantId);

    /** El roleId ya determina el tenant (cada rol pertenece a uno solo, o es el rol de sistema). */
    long countByRoleId(String roleId);

    long countByTenantIdAndEnabledTrue(String tenantId);

    /** Cantidad de cuentas con el rol de sistema (Superadmin) — invariante de plataforma, no por tenant. */
    long countByRoleSystemTrue();
}
