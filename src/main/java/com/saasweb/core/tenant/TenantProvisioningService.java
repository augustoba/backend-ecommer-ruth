package com.saasweb.core.tenant;

import com.saasweb.core.hero.HeroSlide;
import com.saasweb.core.hero.HeroSlideRepository;
import com.saasweb.core.page.PageBlockService;
import com.saasweb.core.param.ParamGroup;
import com.saasweb.core.param.ParamOption;
import com.saasweb.core.param.ParamRepository;
import com.saasweb.core.product.Product;
import com.saasweb.core.product.ProductParam;
import com.saasweb.core.product.ProductRepository;
import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.modules.ropa.SizeScale;
import com.saasweb.modules.ropa.SizeScaleRepository;
import com.saasweb.modules.ropa.SizeStock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Crea un tenant nuevo y lo deja con datos de ejemplo del rubro elegido, para
 * poder mostrarlo funcionando local sin subir nada a mano. Ver PLAN_SAAS.md
 * Fase 9 (plantillas de ferretería/repuestos) y el "selector de tienda modo
 * demo" (`TenantResolutionFilter`) para verlo sin subdominios reales.
 *
 * <p>Ferretería y repuestos no necesitan stock por variante (confirmado con
 * el usuario, ver PLAN_SAAS.md) — para no tocar el checkout/stock que ya
 * funciona, se les siembra una escala de talle trivial de un solo valor
 * ("Único") en vez de generalizar `SizeScale`/`SizeStock`.</p>
 */
@Service
@Transactional
public class TenantProvisioningService {

    private static final String UNICO_SCALE = "Único";

    private final TenantService tenantService;
    private final ParamRepository paramRepo;
    private final SizeScaleRepository sizeScaleRepo;
    private final ProductRepository productRepo;
    private final SiteSettingsService siteSettingsService;
    private final PageBlockService pageBlockService;
    private final HeroSlideRepository heroSlideRepo;

    public TenantProvisioningService(TenantService tenantService, ParamRepository paramRepo,
                                     SizeScaleRepository sizeScaleRepo, ProductRepository productRepo,
                                     SiteSettingsService siteSettingsService, PageBlockService pageBlockService,
                                     HeroSlideRepository heroSlideRepo) {
        this.tenantService = tenantService;
        this.paramRepo = paramRepo;
        this.sizeScaleRepo = sizeScaleRepo;
        this.productRepo = productRepo;
        this.siteSettingsService = siteSettingsService;
        this.pageBlockService = pageBlockService;
        this.heroSlideRepo = heroSlideRepo;
    }

    public Tenant provision(String name, String slug, Rubro rubro) {
        Tenant tenant = tenantService.create(name, slug, rubro);
        String tenantId = tenant.getId();

        siteSettingsService.createFor(tenantId, name, rubro.getDefaultTheme(),
                RubroImages.logo(rubro.getLogoEmoji(), rubro.getLogoColor()));
        pageBlockService.ensureDefaultHomeBlocks(tenantId);
        seedHeroSlides(tenantId, rubro);

        switch (rubro) {
            case ROPA -> seedRopa(tenantId);
            case FERRETERIA -> seedFerreteria(tenantId);
            case REPUESTOS -> seedRepuestos(tenantId);
        }
        return tenant;
    }

    /** Fotos del carrusel de la home, para que una tienda nueva nunca arranque sin carrusel (ver PLAN_SAAS.md Fase 9). */
    private void seedHeroSlides(String tenantId, Rubro rubro) {
        List<RubroImages.Slide> slides = RubroImages.heroSlidesFor(rubro);
        for (int i = 0; i < slides.size(); i++) {
            RubroImages.Slide slide = slides.get(i);
            HeroSlide hs = new HeroSlide();
            hs.setId(UUID.randomUUID().toString());
            hs.setTenantId(tenantId);
            hs.setImageUrl(slide.imageDataUri());
            hs.setAlt(slide.alt());
            hs.setPosition(i);
            heroSlideRepo.save(hs);
        }
    }

    // --- Ropa (genérica — la tienda piloto tiene su propio seed más grande en DataSeeder) ---

    private void seedRopa(String tenantId) {
        ParamOption pubUnisex = opt("Unisex");
        ParamOption pubNene = opt("Nene");
        ParamOption pubNena = opt("Nena");
        ParamGroup publico = group(tenantId, "Público", false, opt("Bebé"), pubNena, pubNene, pubUnisex);

        ParamOption tipoRemera = opt("Remera");
        ParamOption tipoPantalon = opt("Pantalón");
        ParamOption tipoVestido = opt("Vestido");
        ParamGroup tipo = group(tenantId, "Tipo de prenda", false, tipoRemera, tipoPantalon, tipoVestido);

        paramRepo.save(publico);
        paramRepo.save(tipo);

        String scaleId = scale(tenantId, "Talles", List.of("S", "M", "L", "XL"));

        product(tenantId, "Remera básica algodón", "Remera lisa de algodón peinado, corte clásico.",
                "8900", scaleId, "👕", "#86e6bb",
                params(publico.getId(), pubUnisex.getId(), tipo.getId(), tipoRemera.getId()),
                stocks(scaleId, "S:8", "M:10", "L:6", "XL:3"));
        product(tenantId, "Pantalón cargo", "Pantalón cargo con bolsillos laterales, friza reforzada.",
                "15400", scaleId, "👖", "#60a5fa",
                params(publico.getId(), pubNene.getId(), tipo.getId(), tipoPantalon.getId()),
                stocks(scaleId, "S:4", "M:6", "L:5", "XL:2"));
        product(tenantId, "Vestido casual", "Vestido liviano de tela plana, ideal entretiempo.",
                "13800", scaleId, "👗", "#f9a8d4",
                params(publico.getId(), pubNena.getId(), tipo.getId(), tipoVestido.getId()),
                stocks(scaleId, "S:5", "M:5", "L:3", "XL:1"));
    }

    // --- Ferretería (ver PLAN_SAAS.md Fase 9) ---

    private void seedFerreteria(String tenantId) {
        ParamOption matAcero = opt("Acero");
        ParamOption matLaton = opt("Latón");
        ParamGroup material = group(tenantId, "Material", false,
                matAcero, opt("Bronce"), matLaton, opt("Plástico"));

        ParamOption unUnidad = opt("Unidad");
        ParamOption unCaja = opt("Caja");
        ParamGroup unidad = group(tenantId, "Unidad de venta", false,
                unUnidad, unCaja, opt("Metro"), opt("Kilogramo"));

        paramRepo.save(material);
        paramRepo.save(unidad);

        String scaleId = scale(tenantId, UNICO_SCALE, List.of(UNICO_SCALE));

        product(tenantId, "Tornillo autoperforante 8x1\"", "Acero zincado. Caja por 100 unidades.",
                "340", scaleId, "🔩", "#d9a441",
                params(material.getId(), matAcero.getId(), unidad.getId(), unCaja.getId()),
                stocks(scaleId, UNICO_SCALE + ":120"));
        product(tenantId, "Taladro percutor 750W", "Mandril de 13mm, maletín incluido.",
                "89900", scaleId, "🛠️", "#b5502c",
                params(unidad.getId(), unUnidad.getId()),
                stocks(scaleId, UNICO_SCALE + ":8"));
        product(tenantId, "Pintura sintética 1L", "Blanco mate, interior/exterior.",
                "6250", scaleId, "🎨", "#4a4a47",
                params(unidad.getId(), unUnidad.getId()),
                stocks(scaleId, UNICO_SCALE + ":30"));
        product(tenantId, "Candado de seguridad 50mm", "Latón macizo, incluye 3 llaves.",
                "8900", scaleId, "🔒", "#211e1a",
                params(material.getId(), matLaton.getId(), unidad.getId(), unUnidad.getId()),
                stocks(scaleId, UNICO_SCALE + ":15"));
    }

    // --- Repuestos de autos (ver PLAN_SAAS.md Fase 9) ---

    private void seedRepuestos(String tenantId) {
        ParamOption marcaToyota = opt("Toyota");
        ParamOption marcaChevrolet = opt("Chevrolet");
        ParamGroup marca = group(tenantId, "Marca del vehículo", false,
                marcaToyota, marcaChevrolet, opt("Volkswagen"), opt("Fiat"));

        ParamOption catMotor = opt("Motor");
        ParamOption catFrenos = opt("Frenos");
        ParamOption catElectrico = opt("Eléctrico");
        ParamGroup categoria = group(tenantId, "Categoría", false,
                catMotor, catFrenos, opt("Suspensión"), catElectrico);

        paramRepo.save(marca);
        paramRepo.save(categoria);

        String scaleId = scale(tenantId, UNICO_SCALE, List.of(UNICO_SCALE));

        product(tenantId, "Filtro de aceite", "OEM 15208-65F0J. Compatible Corolla 2009-2018.",
                "7800", scaleId, "🧯", "#7c8b5b",
                params(marca.getId(), marcaToyota.getId(), categoria.getId(), catMotor.getId()),
                stocks(scaleId, UNICO_SCALE + ":40"));
        product(tenantId, "Pastillas de freno delanteras", "OEM 04465-52180. Juego x4.",
                "24500", scaleId, "🛑", "#c8372e",
                params(marca.getId(), marcaToyota.getId(), categoria.getId(), catFrenos.getId()),
                stocks(scaleId, UNICO_SCALE + ":18"));
        product(tenantId, "Correa de distribución", "OEM 13568-19166. Corsa 1.6 8v.",
                "18900", scaleId, "⚙️", "#8b93a1",
                params(marca.getId(), marcaChevrolet.getId(), categoria.getId(), catMotor.getId()),
                stocks(scaleId, UNICO_SCALE + ":12"));
        product(tenantId, "Batería 12V 60Ah", "Compatibilidad universal. Entrega usada requerida.",
                "95000", scaleId, "🔋", "#23262b",
                params(categoria.getId(), catElectrico.getId()),
                stocks(scaleId, UNICO_SCALE + ":6"));
    }

    // --- Helpers ---

    private ParamGroup group(String tenantId, String name, boolean multiple, ParamOption... options) {
        ParamGroup g = new ParamGroup();
        g.setId(UUID.randomUUID().toString());
        g.setTenantId(tenantId);
        g.setName(name);
        g.setMultiple(multiple);
        g.setShowInCatalog(true);
        g.setSystem(false);
        for (ParamOption o : options) g.addOption(o);
        return g;
    }

    private ParamOption opt(String label) {
        ParamOption o = new ParamOption();
        o.setId(UUID.randomUUID().toString());
        o.setLabel(label);
        return o;
    }

    /** Devuelve el id de la escala creada. */
    private String scale(String tenantId, String name, List<String> values) {
        SizeScale s = new SizeScale();
        s.setId(UUID.randomUUID().toString());
        s.setTenantId(tenantId);
        s.setName(name);
        s.setSystem(false);
        s.setValues(new ArrayList<>(values));
        sizeScaleRepo.save(s);
        return s.getId();
    }

    private Set<ProductParam> params(String groupId, String optionId) {
        Set<ProductParam> set = new LinkedHashSet<>();
        set.add(new ProductParam(groupId, optionId));
        return set;
    }

    private Set<ProductParam> params(String group1, String opt1, String group2, String opt2) {
        Set<ProductParam> set = new LinkedHashSet<>(params(group1, opt1));
        set.add(new ProductParam(group2, opt2));
        return set;
    }

    private List<SizeStock> stocks(String scaleId, String... pairs) {
        List<SizeStock> list = new ArrayList<>();
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            list.add(new SizeStock(parts[0], Integer.parseInt(parts[1])));
        }
        return list;
    }

    private void product(String tenantId, String name, String description, String price,
                         String sizeScaleId, String emoji, String color,
                         Set<ProductParam> params, List<SizeStock> stocks) {
        Product p = new Product();
        p.setId(UUID.randomUUID().toString());
        p.setTenantId(tenantId);
        p.setName(name);
        p.setDescription(description);
        p.setPrice(new BigDecimal(price));
        p.setCreatedAt(Instant.now());
        p.getImages().add(RubroImages.productIcon(emoji, color));
        p.setActive(true);
        p.setSizeScaleId(sizeScaleId);
        p.getParams().addAll(params);
        p.getSizeStocks().addAll(stocks);
        productRepo.save(p);
    }
}
