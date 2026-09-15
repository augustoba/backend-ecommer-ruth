package com.saasweb.repository;

import com.saasweb.model.ParamGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParamRepository extends JpaRepository<ParamGroup, String> {
}
