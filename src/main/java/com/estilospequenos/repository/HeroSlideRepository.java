package com.estilospequenos.repository;

import com.estilospequenos.model.HeroSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeroSlideRepository extends JpaRepository<HeroSlide, String> {
    List<HeroSlide> findAllByOrderByPositionAsc();
}
