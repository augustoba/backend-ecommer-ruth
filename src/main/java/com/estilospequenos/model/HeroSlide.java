package com.estilospequenos.model;

import jakarta.persistence.*;

/** Foto del carrusel de la home. */
@Entity
@Table(name = "hero_slide")
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
