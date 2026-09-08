package com.estilospequenos.common;

import java.text.Normalizer;
import java.util.UUID;

/** Genera ids legibles a partir de texto (ej: "Ropa niños" → "ropa-ninos"). */
public final class Slugs {

    private Slugs() {}

    public static String slug(String text) {
        String s = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        s = s.length() > 24 ? s.substring(0, 24) : s;
        return s.isEmpty() ? "x" : s;
    }

    public static String shortRandom() {
        return UUID.randomUUID().toString().substring(0, 4);
    }
}
