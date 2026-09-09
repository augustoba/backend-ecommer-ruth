package com.estilospequenos.repository;

import com.estilospequenos.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc();
    List<Product> findByDeletedFalseOrderByCreatedAtDesc();
    List<Product> findByDeletedTrueOrderByCreatedAtDesc();
    List<Product> findBySupplierId(String supplierId);
    long countByActiveTrueAndDeletedFalse();
    long countByDeletedFalse();

    /**
     * Listado del panel con filtros opcionales (todos server-side):
     *  - `search`: texto en el nombre.
     *  - `supplierId`: proveedor exacto.
     *  - `active`: true = solo publicados, false = solo ocultos, null = todos.
     *  - `groupId` + `optionId`: el producto tiene esa opción de parametría.
     *  - `noStock`: true = solo productos con stock total 0.
     * Siempre excluye los archivados (deleted).
     */
    @Query("""
            select p from Product p
            where p.deleted = false
              and (:search is null or lower(p.name) like :searchLike)
              and (:supplierId is null or p.supplierId = :supplierId)
              and (:active is null or p.active = :active)
              and (:groupId is null or exists (
                    select 1 from p.params pp where pp.groupId = :groupId and pp.optionId = :optionId))
              and (:noStock = false or not exists (
                    select 1 from p.sizeStocks ss where ss.stock > 0))
            order by p.createdAt desc
            """)
    Page<Product> search(@Param("search") String search,
                         @Param("searchLike") String searchLike,
                         @Param("supplierId") String supplierId,
                         @Param("active") Boolean active,
                         @Param("groupId") String groupId,
                         @Param("optionId") String optionId,
                         @Param("noStock") boolean noStock,
                         Pageable pageable);
}
