package com.estilospequenos.repository;

import com.estilospequenos.model.ParamGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParamRepository extends JpaRepository<ParamGroup, String> {
}
