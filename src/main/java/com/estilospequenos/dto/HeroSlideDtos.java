package com.estilospequenos.dto;

import com.estilospequenos.model.HeroSlide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class HeroSlideDtos {

    private HeroSlideDtos() {}

    public record SlideRequest(@NotBlank String imageUrl, String alt) {}

    public record ReorderRequest(@NotEmpty List<String> ids) {}

    public record SlideResponse(String id, String imageUrl, String alt) {
        public static SlideResponse from(HeroSlide s) {
            return new SlideResponse(s.getId(), s.getImageUrl(), s.getAlt());
        }
    }
}
