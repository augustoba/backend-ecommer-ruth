package com.estilospequenos.service;

import com.estilospequenos.dto.SiteSettingsDtos.SettingsRequest;
import com.estilospequenos.model.SiteSettings;
import com.estilospequenos.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SiteSettingsService {

    private final SiteSettingsRepository repo;

    public SiteSettingsService(SiteSettingsRepository repo) {
        this.repo = repo;
    }

    /** Devuelve la fila de settings; si no existe, la crea con los valores por defecto. */
    public SiteSettings get() {
        return repo.findById(SiteSettings.SINGLETON_ID).orElseGet(() -> repo.save(defaults()));
    }

    public SiteSettings update(SettingsRequest req) {
        SiteSettings s = get();
        s.setStoreName(req.storeName().trim());
        s.setWhatsappNumber(req.whatsappNumber().trim());
        s.setAboutText(blankToNull(req.aboutText()));
        s.setInstagram(cleanHandle(req.instagram()));
        s.setFacebookUrl(blankToNull(req.facebookUrl()));
        return repo.save(s);
    }

    private static SiteSettings defaults() {
        SiteSettings s = new SiteSettings();
        s.setStoreName("Estilos Pequeños");
        s.setWhatsappNumber("5491122334455");
        s.setAboutText(
                "Somos Estilos Pequeños 🧸 Hace 5 años vestimos a los más chicos con ropa cómoda, "
                        + "de calidad y con onda. Elegimos cada prenda pensando en la comodidad de los peques "
                        + "y la tranquilidad de las familias. ¡Gracias por elegirnos!");
        s.setInstagram("estilospequenos_");
        s.setFacebookUrl("https://www.facebook.com/share/1NZXdYgick/");
        return s;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String cleanHandle(String v) {
        String h = blankToNull(v);
        return h == null ? null : h.replaceFirst("^@", "");
    }
}
