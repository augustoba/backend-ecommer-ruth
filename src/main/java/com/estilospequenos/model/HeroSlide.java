package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Foto del carrusel de la home. */
@Entity
@Table(name = "hero_slide")
@Getter
@Setter
@NoArgsConstructor
public class HeroSlide {

    @Id
    private String id;

    /** URL o data URI de la imagen. */
    @Column(nullable = false, length = 5_000_000)
    private String imageUrl;

    @Column(nullable = false)
    private String alt;

    @Column(nullable = false)
    private int position;
}
