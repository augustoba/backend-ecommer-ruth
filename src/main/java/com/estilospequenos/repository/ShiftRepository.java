package com.estilospequenos.repository;

import com.estilospequenos.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, String> {

    Optional<Shift> findByUserDniAndClosedAtIsNull(String userDni);

    Page<Shift> findAllByOrderByOpenedAtDesc(Pageable pageable);

    Page<Shift> findByUserDniOrderByOpenedAtDesc(String userDni, Pageable pageable);
}
