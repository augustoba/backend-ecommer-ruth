package com.estilospequenos.config;

import com.estilospequenos.discount.Discount;
import com.estilospequenos.discount.DiscountConfig;
import com.estilospequenos.discount.DiscountConfigRepository;
import com.estilospequenos.discount.DiscountRepository;
import com.estilospequenos.param.ParamGroup;
import com.estilospequenos.param.ParamOption;
import com.estilospequenos.param.ParamRepository;
import com.estilospequenos.product.Product;
import com.estilospequenos.product.ProductParam;
import com.estilospequenos.product.ProductRepository;
import com.estilospequenos.product.SizeStock;
import com.estilospequenos.sizescale.SizeScale;
import com.estilospequenos.sizescale.SizeScaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Carga datos de ejemplo la primera vez (si las tablas están vacías). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AppProperties props;
    private final ParamRepository paramRepo;
    private final SizeScaleRepository sizeScaleRepo;
    private final DiscountRepository discountRepo;
    private final DiscountConfigRepository discountConfigRepo;
    private final ProductRepository productRepo;

    @Override
    public void run(String... args) {
        if (!props.getSeed().isEnabled()) return;
        seedParamGroups();
        seedSizeScales();
        seedDiscounts();
        seedProducts();
    }

    // --- Parametrías ---

    private void seedParamGroups() {
        if (paramRepo.count() > 0) return;
        paramRepo.save(group("grp-publico", "Público", false, true, true, List.of(
                opt("publico-bebe", "Bebé"), opt("publico-nena", "Nena"),
                opt("publico-nene", "Nene"), opt("publico-unisex", "Unisex"))));
        paramRepo.save(group("grp-tipo", "Tipo de prenda", false, true, false, List.of(
                opt("tipo-remera", "Remera"), opt("tipo-buzo", "Buzo / Campera"),
                opt("tipo-pantalon", "Pantalón"), opt("tipo-jean", "Jean"),
                opt("tipo-vestido", "Vestido / Pollera"), opt("tipo-body", "Body / Enterito"),
                opt("tipo-conjunto", "Conjunto"), opt("tipo-calzado", "Calzado"),
                opt("tipo-accesorio", "Accesorio"))));
        paramRepo.save(group("grp-estacion", "Estación", true, true, false, List.of(
                opt("estacion-primavera", "Primavera"), opt("estacion-verano", "Verano"),
                opt("estacion-otono", "Otoño"), opt("estacion-invierno", "Invierno"),
                opt("estacion-todo", "Todo el año"))));
        log.info("Seed: 3 parametrías cargadas.");
    }

    private ParamGroup group(String id, String name, boolean multiple, boolean showInCatalog,
                             boolean system, List<ParamOption> options) {
        ParamGroup g = new ParamGroup();
        g.setId(id);
        g.setName(name);
        g.setMultiple(multiple);
        g.setShowInCatalog(showInCatalog);
        g.setSystem(system);
        options.forEach(g::addOption);
        return g;
    }

    private ParamOption opt(String id, String label) {
        ParamOption o = new ParamOption();
        o.setId(id);
        o.setLabel(label);
        return o;
    }

    // --- Escalas de talle ---

    private void seedSizeScales() {
        if (sizeScaleRepo.count() > 0) return;
        sizeScaleRepo.save(scale("escala-bebe", "Ropa bebé (por edad)",
                List.of("RN", "0-3M", "3-6M", "6-12M", "12-18M", "18-24M", "24M")));
        sizeScaleRepo.save(scale("escala-ninos", "Ropa niños",
                List.of("1", "2", "3", "4", "6", "8", "10", "12", "14", "16")));
        sizeScaleRepo.save(scale("escala-adultos", "Ropa adultos",
                List.of("XS", "S", "M", "L", "XL", "XXL")));
        sizeScaleRepo.save(scale("escala-calzado-ninos", "Calzado niños", range(17, 34)));
        sizeScaleRepo.save(scale("escala-calzado-adultos", "Calzado adultos", range(34, 46)));
        log.info("Seed: 5 escalas de talle cargadas.");
    }

    private SizeScale scale(String id, String name, List<String> values) {
        SizeScale s = new SizeScale();
        s.setId(id);
        s.setName(name);
        s.setSystem(true);
        s.setValues(new ArrayList<>(values));
        return s;
    }

    private static List<String> range(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(String::valueOf).collect(Collectors.toList());
    }

    // --- Descuentos ---

    private void seedDiscounts() {
        if (discountRepo.count() == 0) {
            discountRepo.save(montoTier("100000", 20));
            discountRepo.save(montoTier("200000", 25));
            log.info("Seed: 2 descuentos por monto cargados.");
        }
        if (discountConfigRepo.count() == 0) {
            discountConfigRepo.save(new DiscountConfig());
        }
    }

    private Discount montoTier(String minAmount, int percent) {
        Discount d = new Discount();
        d.setId(UUID.randomUUID().toString());
        d.setKind(Discount.Kind.MONTO);
        d.setMinAmount(new BigDecimal(minAmount));
        d.setDiscountPercent(percent);
        d.setEnabled(true);
        return d;
    }

    // --- Productos de ejemplo ---

    private void seedProducts() {
        if (productRepo.count() > 0) return;

        productRepo.save(product("Body manga larga estampado animales",
                "Body de algodón suave, manga larga, con estampa de animalitos. Ideal para el día a día.",
                "9800", "0 a 12 meses", "escala-bebe", "🧸", "#fdba74",
                params("publico-bebe", "tipo-body", List.of("estacion-todo")),
                stocks("RN:4", "0-3M:3", "3-6M:4", "6-12M:3")));

        productRepo.save(product("Conjunto jogging campera + pantalón",
                "Conjunto de frisa perchada, campera con capucha y pantalón con puños. Súper abrigado.",
                "24500", "2 a 8 años", "escala-ninos", "🧥", "#85d6ff",
                params("publico-nene", "tipo-conjunto", List.of("estacion-otono", "estacion-invierno")),
                stocks("2:2", "3:2", "4:3", "6:1", "8:1")));

        productRepo.save(product("Vestido plumeti volados",
                "Vestido liviano de tela plumeti con volados en el ruedo y moño en la espalda.",
                "19900", "2 a 10 años", "escala-ninos", "👗", "#f9a8d4",
                params("publico-nena", "tipo-vestido", List.of("estacion-primavera", "estacion-verano")),
                stocks("2:2", "3:2", "4:3", "6:2", "8:1", "10:1")));

        productRepo.save(product("Remera básica algodón (pack x3)",
                "Pack de 3 remeras lisas de algodón peinado en colores surtidos. Unisex.",
                "15600", "1 a 12 años", "escala-ninos", "👕", "#86e6bb",
                params("publico-unisex", "tipo-remera", List.of("estacion-todo")),
                stocks("1:3", "2:3", "3:3", "4:4", "6:2", "8:2", "10:2", "12:1")));

        productRepo.save(product("Jean chupín con elástico",
                "Jean chupín de tiro medio con cintura elastizada para mayor comodidad.",
                "21300", "2 a 12 años", "escala-ninos", "👖", "#60a5fa",
                params("publico-nena", "tipo-jean", List.of("estacion-otono", "estacion-invierno", "estacion-primavera")),
                stocks("2:1", "3:1", "4:2", "6:1", "8:1", "10:1", "12:0")));

        productRepo.save(product("Buzo canguro dinosaurios",
                "Buzo canguro de frisa con bolsillo y estampa de dinosaurios.",
                "18200", "2 a 10 años", "escala-ninos", "🦕", "#fde68a",
                params("publico-nene", "tipo-buzo", List.of("estacion-otono", "estacion-invierno")),
                stocks("2:0", "3:0", "4:0", "6:0", "8:0", "10:0")));

        productRepo.save(product("Enterito corto verano",
                "Enterito liviano de algodón, ideal para el verano, con broches en la entrepierna.",
                "13400", "3 a 24 meses", "escala-bebe", "🌞", "#fca5a5",
                params("publico-bebe", "tipo-body", List.of("estacion-primavera", "estacion-verano")),
                stocks("3-6M:5", "6-12M:6", "12-18M:3", "18-24M:2")));

        productRepo.save(product("Campera inflable con capucha",
                "Campera inflable liviana, abrigada, con capucha desmontable. Repelente al agua.",
                "32900", "4 a 14 años", "escala-ninos", "🧥", "#a5b4fc",
                params("publico-unisex", "tipo-buzo", List.of("estacion-otono", "estacion-invierno")),
                stocks("4:1", "6:1", "8:1", "10:1", "12:1", "14:0")));

        productRepo.save(product("Pollera short con volado",
                "Pollera short de gabardina liviana con volado, cintura con elástico.",
                "12800", "2 a 10 años", "escala-ninos", "🩳", "#f0abfc",
                params("publico-nena", "tipo-vestido", List.of("estacion-primavera", "estacion-verano")),
                stocks("2:2", "3:2", "4:2", "6:2", "8:1", "10:1")));

        productRepo.save(product("Zapatillas urbanas velcro",
                "Zapatillas livianas con cierre de velcro, suela antideslizante.",
                "27500", "1 a 8 años", "escala-calzado-ninos", "👟", "#d8b4fe",
                params("publico-unisex", "tipo-calzado", List.of("estacion-todo")),
                stocks("22:1", "24:2", "26:2", "28:1", "30:1", "32:1")));

        log.info("Seed: 10 productos de ejemplo cargados.");
    }

    private Product product(String name, String description, String price, String ageRange,
                            String sizeScaleId, String emoji, String color,
                            Set<ProductParam> params, List<SizeStock> stocks) {
        Product p = new Product();
        p.setId(UUID.randomUUID().toString());
        p.setName(name);
        p.setDescription(description);
        p.setPrice(new BigDecimal(price));
        p.setAgeRange(ageRange);
        p.setImageUrl(iconDataUri(emoji, color));
        p.setActive(true);
        p.setSizeScaleId(sizeScaleId);
        p.getParams().addAll(params);
        p.getSizeStocks().addAll(stocks);
        return p;
    }

    private Set<ProductParam> params(String publico, String tipo, List<String> estaciones) {
        Set<ProductParam> set = new LinkedHashSet<>();
        set.add(new ProductParam("grp-publico", publico));
        set.add(new ProductParam("grp-tipo", tipo));
        for (String e : estaciones) set.add(new ProductParam("grp-estacion", e));
        return set;
    }

    private List<SizeStock> stocks(String... pairs) {
        List<SizeStock> list = new ArrayList<>();
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            list.add(new SizeStock(parts[0], Integer.parseInt(parts[1])));
        }
        return list;
    }

    /** SVG mínimo (emoji sobre círculo de color) como data URI, para el catálogo de ejemplo. */
    private String iconDataUri(String emoji, String color) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='400' height='500' viewBox='0 0 400 500'>"
                + "<rect width='400' height='500' fill='#fff7ed'/>"
                + "<circle cx='200' cy='250' r='150' fill='" + color + "'/>"
                + "<text x='200' y='300' font-size='140' text-anchor='middle'>" + emoji + "</text></svg>";
        return "data:image/svg+xml;charset=utf-8," + URLEncoder.encode(svg, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
