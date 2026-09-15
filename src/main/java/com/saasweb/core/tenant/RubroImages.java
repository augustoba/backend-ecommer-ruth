package com.saasweb.core.tenant;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Genera imágenes de ejemplo (SVG como data URI) para no depender de subir
 * fotos reales a Cloudinary — pensado para que una tienda nueva (o la
 * piloto) nunca se muestre sin fotos en el carrusel ni en el catálogo. Lo
 * usan {@code TenantProvisioningService} (tiendas nuevas) y
 * {@code DataSeeder} (completa las que ya existían sin carrusel, al
 * arrancar el backend — ver PLAN_SAAS.md Fase 9).
 */
public final class RubroImages {

    private RubroImages() {
    }

    /** Foto de producto: círculo de color con el emoji, sobre fondo claro. */
    public static String productIcon(String emoji, String color) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='400' height='500' viewBox='0 0 400 500'>"
                + "<rect width='400' height='500' fill='#f5f5f4'/>"
                + "<circle cx='200' cy='250' r='150' fill='" + color + "'/>"
                + "<text x='200' y='300' font-size='140' text-anchor='middle'>" + emoji + "</text></svg>";
        return toDataUri(svg);
    }

    /** Logo genérico (círculo de color con el emoji del rubro) para `SiteSettings.logoUrl`. */
    public static String logo(String emoji, String color) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200' viewBox='0 0 200 200'>"
                + "<circle cx='100' cy='100' r='100' fill='" + color + "'/>"
                + "<text x='100' y='128' font-size='100' text-anchor='middle'>" + emoji + "</text></svg>";
        return toDataUri(svg);
    }

    /** Banner ancho (21:9 aprox) para el carrusel de la home, con degradé + texto + emojis decorativos. */
    public static String heroBanner(String headline, String subtitle, String[] emojis,
                             String colorFrom, String colorTo, String textColor) {
        StringBuilder deco = new StringBuilder();
        int[] xs = {1220, 1430, 1080};
        int[] ys = {190, 430, 460};
        int[] sizes = {210, 170, 140};
        double[] opacities = {0.18, 0.15, 0.12};
        for (int i = 0; i < emojis.length && i < xs.length; i++) {
            deco.append("<text x='").append(xs[i]).append("' y='").append(ys[i])
                    .append("' font-size='").append(sizes[i])
                    .append("' opacity='").append(opacities[i])
                    .append("'>").append(emojis[i]).append("</text>");
        }
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='1680' height='720' viewBox='0 0 1680 720'>"
                + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0' stop-color='" + colorFrom + "'/>"
                + "<stop offset='1' stop-color='" + colorTo + "'/>"
                + "</linearGradient></defs>"
                + "<rect width='1680' height='720' fill='url(#g)'/>"
                + deco
                + "<text x='100' y='330' font-family='Arial, sans-serif' font-weight='800' font-size='68' fill='"
                + textColor + "'>" + headline + "</text>"
                + "<text x='100' y='400' font-family='Arial, sans-serif' font-size='34' fill='" + textColor
                + "' opacity='0.92'>" + subtitle + "</text>"
                + "</svg>";
        return toDataUri(svg);
    }

    /** Foto/imagen + alt para una foto del carrusel de la home. */
    public record Slide(String imageDataUri, String alt) {
    }

    /** Las 2 fotos de carrusel con las que arranca una tienda nueva de este rubro. */
    public static List<Slide> heroSlidesFor(Rubro rubro) {
        return switch (rubro) {
            case FERRETERIA -> List.of(
                    new Slide(heroBanner("Todo para tu obra y tu casa",
                            "Herramientas y materiales de calidad, al mejor precio",
                            new String[]{"🔧", "🔩", "🛠️"}, "#c27a34", "#6b3818", "#fff8ee"),
                            "Herramientas y materiales de ferretería"),
                    new Slide(heroBanner("Ofertas de la semana",
                            "Descuentos en pinturas, tornillería y accesorios",
                            new String[]{"🎨", "🪛", "🔩"}, "#a85f27", "#4a260f", "#fff8ee"),
                            "Ofertas de ferretería"));
            case REPUESTOS -> List.of(
                    new Slide(heroBanner("Repuestos originales al mejor precio",
                            "Para todas las marcas, con garantía",
                            new String[]{"⚙️", "🔋", "🛑"}, "#55788b", "#1a2e38", "#eef4f7"),
                            "Repuestos de auto originales"),
                    new Slide(heroBanner("Envío a todo el país",
                            "Compatibilidad garantizada con tu vehículo",
                            new String[]{"🚗", "🧯", "⚙️"}, "#35586b", "#131f26", "#eef4f7"),
                            "Envíos de repuestos a todo el país"));
            case ROPA -> List.of(
                    new Slide(heroBanner("Moda cómoda para los más chicos",
                            "Nueva colección disponible",
                            new String[]{"👕", "👗", "🧸"}, "#fb923c", "#c2410c", "#fff8ee"),
                            "Nueva colección de ropa infantil"),
                    new Slide(heroBanner("Envío gratis en compras +$30.000",
                            "Zona microcentro",
                            new String[]{"🎁", "🚚", "👶"}, "#f97316", "#9a3412", "#fff8ee"),
                            "Envío gratis en compras grandes"));
        };
    }

    private static String toDataUri(String svg) {
        return "data:image/svg+xml;charset=utf-8," + URLEncoder.encode(svg, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
