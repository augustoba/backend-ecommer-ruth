package com.saasweb.core.tenant;

import com.saasweb.common.BadRequestException;
import com.saasweb.core.admin.AdminUserRepository;
import com.saasweb.core.admin.RoleRepository;
import com.saasweb.core.coupon.CouponRepository;
import com.saasweb.core.discount.DiscountRepository;
import com.saasweb.core.exchange.ExchangeRepository;
import com.saasweb.core.hero.HeroSlideRepository;
import com.saasweb.core.marketing.MarketingConfigRepository;
import com.saasweb.core.marketing.MarketingSendRepository;
import com.saasweb.core.order.OrderRepository;
import com.saasweb.core.page.PageBlockRepository;
import com.saasweb.core.param.ParamRepository;
import com.saasweb.core.product.ProductRepository;
import com.saasweb.core.settings.SiteSettingsRepository;
import com.saasweb.core.shift.ShiftRepository;
import com.saasweb.core.supplier.SupplierRepository;
import com.saasweb.modules.ropa.SizeScaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borrado permanente de un tenant y TODOS sus datos — irreversible. Pensado
 * para el botón "Eliminar tienda" del superadmin (confirmación en el
 * frontend: hay que tipear el slug exacto antes de habilitar el botón).
 *
 * Recorre cada tabla propia del tenant y la vacía antes de borrar la fila del
 * tenant en sí — orden pensado para no romper ninguna FK:
 *  - Order/Exchange/ParamGroup se borran entidad-por-entidad (no bulk SQL),
 *    así Hibernate respeta el `cascade=ALL, orphanRemoval=true` hacia sus
 *    líneas/opciones (ver OrderLine/ExchangeLine/ParamOption).
 *  - AdminUser se borra ANTES que Role: `AdminUser.role` es un
 *    `@ManyToOne` sin cascade, así que un Role ya borrado rompería esa FK.
 *  - El rol de sistema (superadmin, `tenantId` null) nunca se toca — todos
 *    los `deleteAllByTenantId` filtran por el id exacto de este tenant.
 *
 * Lo que este borrado NO puede hacer: eliminar el logo/carrusel/fotos de
 * Cloudinary — el preset unsigned que usa esta plataforma no tiene permiso
 * para borrar assets (hace falta la API key/secret firmada, que no está
 * configurada). Esos archivos quedan huérfanos en la cuenta de Cloudinary;
 * hay que borrarlos a mano ahí si hace falta liberar espacio.
 */
@Service
public class TenantDeletionService {

    private final TenantRepository tenantRepo;
    private final SiteSettingsRepository siteSettingsRepo;
    private final MarketingConfigRepository marketingConfigRepo;
    private final OrderRepository orderRepo;
    private final ExchangeRepository exchangeRepo;
    private final ParamRepository paramRepo;
    private final ProductRepository productRepo;
    private final CouponRepository couponRepo;
    private final DiscountRepository discountRepo;
    private final HeroSlideRepository heroSlideRepo;
    private final MarketingSendRepository marketingSendRepo;
    private final PageBlockRepository pageBlockRepo;
    private final ShiftRepository shiftRepo;
    private final SupplierRepository supplierRepo;
    private final SizeScaleRepository sizeScaleRepo;
    private final AdminUserRepository adminUserRepo;
    private final RoleRepository roleRepo;

    public TenantDeletionService(TenantRepository tenantRepo, SiteSettingsRepository siteSettingsRepo,
            MarketingConfigRepository marketingConfigRepo, OrderRepository orderRepo,
            ExchangeRepository exchangeRepo, ParamRepository paramRepo, ProductRepository productRepo,
            CouponRepository couponRepo, DiscountRepository discountRepo, HeroSlideRepository heroSlideRepo,
            MarketingSendRepository marketingSendRepo, PageBlockRepository pageBlockRepo,
            ShiftRepository shiftRepo, SupplierRepository supplierRepo, SizeScaleRepository sizeScaleRepo,
            AdminUserRepository adminUserRepo, RoleRepository roleRepo) {
        this.tenantRepo = tenantRepo;
        this.siteSettingsRepo = siteSettingsRepo;
        this.marketingConfigRepo = marketingConfigRepo;
        this.orderRepo = orderRepo;
        this.exchangeRepo = exchangeRepo;
        this.paramRepo = paramRepo;
        this.productRepo = productRepo;
        this.couponRepo = couponRepo;
        this.discountRepo = discountRepo;
        this.heroSlideRepo = heroSlideRepo;
        this.marketingSendRepo = marketingSendRepo;
        this.pageBlockRepo = pageBlockRepo;
        this.shiftRepo = shiftRepo;
        this.supplierRepo = supplierRepo;
        this.sizeScaleRepo = sizeScaleRepo;
        this.adminUserRepo = adminUserRepo;
        this.roleRepo = roleRepo;
    }

    /**
     * Borra el tenant y todos sus datos. {@code confirmSlug} tiene que ser
     * exactamente el slug de la tienda — es la confirmación que ya hizo
     * escribir el frontend, pero se vuelve a validar acá porque este
     * endpoint es irreversible y no hay que confiar sólo en el cliente.
     */
    @Transactional
    public void deleteTenant(String tenantId, String confirmSlug) {
        Tenant t = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new BadRequestException("La tienda no existe."));
        if (confirmSlug == null || !confirmSlug.trim().equals(t.getSlug())) {
            throw new BadRequestException("El identificador escrito no coincide con el de la tienda.");
        }

        orderRepo.deleteAllByTenantId(tenantId);
        exchangeRepo.deleteAllByTenantId(tenantId);
        paramRepo.deleteAllByTenantId(tenantId);
        productRepo.deleteAllByTenantId(tenantId);
        couponRepo.deleteAllByTenantId(tenantId);
        discountRepo.deleteAllByTenantId(tenantId);
        heroSlideRepo.deleteAllByTenantId(tenantId);
        marketingSendRepo.deleteAllByTenantId(tenantId);
        pageBlockRepo.deleteAllByTenantId(tenantId);
        shiftRepo.deleteAllByTenantId(tenantId);
        supplierRepo.deleteAllByTenantId(tenantId);
        sizeScaleRepo.deleteAllByTenantId(tenantId);

        // AdminUser antes que Role (ver javadoc de la clase).
        adminUserRepo.deleteAllByTenantId(tenantId);
        roleRepo.deleteAllByTenantId(tenantId);

        if (siteSettingsRepo.existsById(tenantId)) siteSettingsRepo.deleteById(tenantId);
        if (marketingConfigRepo.existsById(tenantId)) marketingConfigRepo.deleteById(tenantId);

        tenantRepo.delete(t);
    }
}
