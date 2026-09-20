package com.estilospequenos.config;

import com.estilospequenos.dto.CouponDtos;
import com.estilospequenos.dto.ExchangeDtos;
import com.estilospequenos.dto.ExpenseBudgetDtos;
import com.estilospequenos.dto.ExpenseDtos;
import com.estilospequenos.dto.OrderDtos;
import com.estilospequenos.dto.SupplierDtos;
import com.estilospequenos.model.DeliveryMethod;
import com.estilospequenos.model.MarketingSend;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.PaymentMethod;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.SizeStock;
import com.estilospequenos.model.Supplier;
import com.estilospequenos.repository.MarketingSendRepository;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.ProductRepository;
import com.estilospequenos.service.CouponService;
import com.estilospequenos.service.ExchangeService;
import com.estilospequenos.service.ExpenseBudgetService;
import com.estilospequenos.service.ExpenseService;
import com.estilospequenos.service.OrderService;
import com.estilospequenos.service.ProductService;
import com.estilospequenos.service.ShiftService;
import com.estilospequenos.service.SupplierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Datos de DEMO para probar las pantallas con volumen realista: ~350 pedidos
 * repartidos en 12 meses, con clientes, proveedores, gastos, cupones, turnos,
 * campañas y movimientos de stock.
 *
 * <p><b>Sólo para desarrollo.</b> Se activa con {@code SEED_DEMO_ENABLED=true}
 * (o {@code app.seed.demo.enabled}) y está apagado por default. Nunca lo pongas
 * en un deploy: escribe cientos de filas.</p>
 *
 * <p><b>Por qué en Java y no en SQL:</b> los pedidos se crean y confirman con
 * {@link OrderService} —los servicios reales—, así el stock queda descontado, los
 * movimientos de stock registrados y el costo congelado en
 * {@code OrderLine.costPrice} tal como en producción. Un INSERT a mano tendría
 * que replicar toda esa lógica y cualquier desvío se vería como un número
 * raro en métricas o balance. Lo único que se hace por fuera es reescribir las
 * fechas ({@link OrderRepository#backdate}), porque el servicio las fija con
 * {@code now()} y acá hacen falta repartidas en el año.</p>
 *
 * <p><b>Idempotente:</b> los clientes de demo usan el dominio
 * {@value #EMAIL_DOMAIN}; si ya hay pedidos con ese dominio, no hace nada.</p>
 */
@Component
@org.springframework.core.annotation.Order(2) // después de DataSeeder (@Order(1)): necesita los productos ya cargados
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Dominio de mail que marca los datos de demo (y hace idempotente el seeder). */
    static final String EMAIL_DOMAIN = "demo.local";

    /** La dueña de la tienda: es quien "vende" y "cobra" en todos los pedidos de demo. */
    private static final String RUTH_DNI = "11111111";

    /** Semilla fija: el mismo dataset en cualquier máquina, para poder comparar. */
    private static final long RANDOM_SEED = 20260920L;

    private static final int MONTHS = 12;

    private static final String[] CLIENTES = {
            "María González", "Lucía Fernández", "Sofía Rodríguez", "Julieta López",
            "Camila Martínez", "Valentina Pérez", "Agustina Gómez", "Martina Díaz",
            "Florencia Sánchez", "Micaela Romero", "Antonella Sosa", "Belén Torres",
            "Rocío Álvarez", "Milagros Ruiz", "Guadalupe Benítez", "Yamila Acosta",
            "Natalia Medina", "Carla Herrera", "Daniela Suárez", "Mariana Aguirre",
            "Paula Quiroga", "Verónica Molina", "Silvina Paz", "Andrea Coronel",
            "Pablo Giménez", "Diego Figueroa", "Santiago Cabrera", "Nicolás Ferreyra",
    };

    private static final String[] CALLES = {
            "San Martín", "Belgrano", "Sarmiento", "Rivadavia", "Mitre",
            "Córdoba", "Maipú", "Junín", "Catamarca", "Salta",
    };

    private static final String[][] PROVEEDORES = {
            {"Distribuidora Norte", "381-4551122", "Av. Roca 1200, San Miguel de Tucumán", "Ropa de bebé y niños"},
            {"Textil Andina", "381-4223344", "Córdoba 850, San Miguel de Tucumán", "Buzos y camperas"},
            {"Calzados del Sur", "381-4667788", "24 de Septiembre 340, San Miguel de Tucumán", "Calzado"},
            {"Importadora Luna", "381-4112200", "Av. Alem 1550, Yerba Buena", "Accesorios y novedades"},
    };

    /**
     * Gastos fijos que se repiten todos los meses: {optionId de la parametría
     * "Categoría de gasto", monto, descripción}. Se referencia por <b>id</b> y no
     * por etiqueta: las etiquetas son editables desde el panel y una diferencia
     * de texto dejaría el gasto sin categoría.
     */
    private static final String[][] GASTOS_FIJOS = {
            {"alquiler", "250000", "Alquiler del local"},
            {"servicios", "48000", "Luz, agua e internet"},
            {"sueldos", "380000", "Sueldo vendedora"},
            {"marketing", "35000", "Redes y publicidad"},
    };

    /** Gastos sueltos: {optionId, monto mínimo, monto máximo, descripción}. */
    private static final String[][] GASTOS_VARIOS = {
            {"mercaderia", "8000", "45000", "Bolsas, etiquetas y perchas"},
            {"otros", "15000", "90000", "Arreglos del local"},
            {"impuestos", "22000", "70000", "Ingresos brutos"},
            {"envios", "5000", "30000", "Envíos de pedidos online"},
    };

    private final AppProperties props;
    private final OrderService orderService;
    private final ProductService productService;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final SupplierService supplierService;
    private final CouponService couponService;
    private final ExpenseService expenseService;
    private final ExpenseBudgetService budgetService;
    private final ExchangeService exchangeService;
    private final ShiftService shiftService;
    private final MarketingSendRepository marketingSendRepo;
    private final TransactionTemplate tx;

    public DemoDataSeeder(AppProperties props,
                          OrderService orderService,
                          ProductService productService,
                          ProductRepository productRepo,
                          OrderRepository orderRepo,
                          SupplierService supplierService,
                          CouponService couponService,
                          ExpenseService expenseService,
                          ExpenseBudgetService budgetService,
                          ExchangeService exchangeService,
                          ShiftService shiftService,
                          MarketingSendRepository marketingSendRepo,
                          PlatformTransactionManager txManager) {
        this.props = props;
        this.orderService = orderService;
        this.productService = productService;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.supplierService = supplierService;
        this.couponService = couponService;
        this.expenseService = expenseService;
        this.budgetService = budgetService;
        this.exchangeService = exchangeService;
        this.shiftService = shiftService;
        this.marketingSendRepo = marketingSendRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Un pedido a backdatear, para aplicar todas las fechas juntas al final. */
    private record Backdate(String orderId, LocalDateTime createdAt, LocalDateTime processedAt) {}

    @Override
    public void run(String... args) {
        if (!props.getSeed().getDemo().isEnabled()) return;

        if (orderRepo.existsByCustomerEmailEndingWith("@" + EMAIL_DOMAIN)) {
            log.info("Seeder de demo: los datos ya estaban cargados, no hago nada.");
            return;
        }

        // findActive() usa @EntityGraph, así que sizeStocks viene cargado: acá
        // no hay sesión abierta (no es una request) y una colección lazy
        // explotaría con LazyInitializationException.
        List<Product> productos = productService.findActive().stream()
                .filter(p -> p.getSizeStocks().stream().anyMatch(s -> s.getStock() > 0))
                .toList();
        if (productos.isEmpty()) {
            log.warn("Seeder de demo: no hay productos activos con stock. "
                    + "Arrancá una vez con SEED_ENABLED=true para tener el catálogo de ejemplo.");
            return;
        }

        log.info("Seeder de demo: generando datos de prueba ({} productos base, {} meses)…",
                productos.size(), MONTHS);
        long t0 = System.currentTimeMillis();

        Random rnd = new Random(RANDOM_SEED);
        List<Supplier> proveedores = seedProveedores();
        seedCostos(productos, proveedores, rnd);
        // Stock inicial: el stock pudo quedar bajo por una corrida anterior del
        // seeder (o por los pedidos de prueba de la base). Sin esto, el primer
        // mes fallaría entero por falta de stock antes de la primera reposición.
        reponer(productos, proveedores, rnd, productos.size() * 10);
        seedGastos(rnd);
        seedCupones(rnd);

        List<Backdate> pendientes = new ArrayList<>();
        int pedidos = seedPedidos(productos, proveedores, rnd, pendientes);
        seedTurnos();
        seedCambios(productos, rnd);
        seedCampanias(rnd);

        // Todas las fechas, en una sola transacción y al final (ver Backdate).
        tx.executeWithoutResult(status -> {
            for (Backdate b : pendientes) {
                orderRepo.backdate(b.orderId(), b.createdAt(), b.processedAt());
                productRepo.backdateMovementsByReference(b.orderId(), b.processedAt());
            }
        });

        log.info("Seeder de demo: listo — {} pedidos y {} fechas reescritas en {} ms. "
                        + "Los datos de demo se identifican por el mail @{}.",
                pedidos, pendientes.size(), System.currentTimeMillis() - t0, EMAIL_DOMAIN);
    }

    // ------------------------------------------------------------------
    //  Proveedores y costos
    // ------------------------------------------------------------------

    /**
     * Crea los proveedores que falten. Se buscan por nombre para que una corrida
     * interrumpida a la mitad no los duplique: el seeder no es transaccional a
     * propósito (cada pedido va en su propia transacción), así que puede quedar
     * a medias y volver a arrancar.
     */
    private List<Supplier> seedProveedores() {
        List<Supplier> ya = supplierService.findAll();
        List<Supplier> out = new ArrayList<>();
        for (String[] p : PROVEEDORES) {
            Optional<Supplier> existente = ya.stream()
                    .filter(s -> s.getName().equalsIgnoreCase(p[0]))
                    .findFirst();
            out.add(existente.orElseGet(() -> supplierService.create(
                    new SupplierDtos.SupplierRequest(p[0], p[1], p[2], p[3]))));
        }
        return out;
    }

    /** Le asigna proveedor y costo a cada producto (si no tenía), para que el margen y el desglose por proveedor tengan datos. */
    private void seedCostos(List<Product> productos, List<Supplier> proveedores, Random rnd) {
        for (int i = 0; i < productos.size(); i++) {
            Product p = productos.get(i);
            boolean cambio = false;
            if (p.getSupplierId() == null && !proveedores.isEmpty()) {
                p.setSupplierId(proveedores.get(i % proveedores.size()).getId());
                cambio = true;
            }
            if (p.getCostPrice() == null) {
                // Costo = ~55-65% del precio de venta, redondeado a 2 decimales.
                int pct = 55 + rnd.nextInt(11);
                p.setCostPrice(p.getPrice()
                        .multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                cambio = true;
            }
            if (cambio) productRepo.save(p);
        }
    }

    // ------------------------------------------------------------------
    //  Gastos y presupuestos
    // ------------------------------------------------------------------

    private void seedGastos(Random rnd) {
        LocalDate hoy = LocalDate.now();

        // Fijos: uno por mes, los últimos 12 meses.
        for (int m = MONTHS - 1; m >= 0; m--) {
            LocalDate mes = YearMonth.from(hoy).minusMonths(m).atDay(Math.min(3, hoy.getDayOfMonth()));
            for (String[] g : GASTOS_FIJOS) {
                expenseService.create(new ExpenseDtos.ExpenseRequest(
                        mes, g[0],
                        new BigDecimal(g[1]), g[2] + " (" + mes.getMonthValue() + "/" + mes.getYear() + ")",
                        false));
            }
        }

        // Sueltos: 1-3 por mes, montos variables.
        for (int m = MONTHS - 1; m >= 0; m--) {
            YearMonth ym = YearMonth.from(hoy).minusMonths(m);
            int cuantos = 1 + rnd.nextInt(3);
            for (int i = 0; i < cuantos; i++) {
                String[] g = GASTOS_VARIOS[rnd.nextInt(GASTOS_VARIOS.length)];
                int dia = 1 + rnd.nextInt(ym.lengthOfMonth());
                int min = Integer.parseInt(g[1]);
                int monto = min + rnd.nextInt(Integer.parseInt(g[2]) - min);
                expenseService.create(new ExpenseDtos.ExpenseRequest(
                        ym.atDay(dia), g[0], BigDecimal.valueOf(monto), g[3], false));
            }
        }

        // Presupuesto mensual para las categorías que más se gastan.
        for (String[] g : GASTOS_FIJOS) {
            budgetService.upsert(new ExpenseBudgetDtos.BudgetRequest(
                    g[0], new BigDecimal(g[1]).multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP)));
        }
    }

    // ------------------------------------------------------------------
    //  Cupones
    // ------------------------------------------------------------------

    private void seedCupones(Random rnd) {
        couponService.create(new CouponDtos.CouponRequest(
                "BIENVENIDA10", null, null, com.estilospequenos.model.Coupon.Kind.PERCENT,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20000), null, null, true, true,
                "10% en tu primera compra"));
        couponService.create(new CouponDtos.CouponRequest(
                "VERANO25", null, null, com.estilospequenos.model.Coupon.Kind.PERCENT,
                BigDecimal.valueOf(25), BigDecimal.valueOf(80000), 100, LocalDate.now().plusMonths(3),
                true, false, "25% en compras sobre $80.000"));
        // Un lote de cupones de campaña, como los que genera MarketingCampaignService.
        couponService.create(new CouponDtos.CouponRequest(
                null, 12, "DEMO", com.estilospequenos.model.Coupon.Kind.PERCENT,
                BigDecimal.valueOf(15), BigDecimal.valueOf(30000), 1,
                LocalDate.now().plusDays(30), true, true, "Cupón de campaña (demo)"));
    }

    // ------------------------------------------------------------------
    //  Pedidos
    // ------------------------------------------------------------------

    private int seedPedidos(List<Product> productos, List<Supplier> proveedores, Random rnd,
                            List<Backdate> pendientes) {
        LocalDate hoy = LocalDate.now();
        int total = 0;
        int fallidos = 0;

        for (int m = MONTHS - 1; m >= 0; m--) {
            YearMonth ym = YearMonth.from(hoy).minusMonths(m);
            int maxDia = (m == 0) ? hoy.getDayOfMonth() : ym.lengthOfMonth();
            // Meses más viejos con menos volumen, y un pico en los últimos:
            // así las comparativas mes a mes tienen forma en vez de ser planas.
            int base = 18 + (MONTHS - 1 - m) * 2;
            int cuantos = base + rnd.nextInt(9);

            for (int i = 0; i < cuantos; i++) {
                int dia = 1 + rnd.nextInt(maxDia);
                LocalDateTime cuando = ym.atDay(dia).atTime(10 + rnd.nextInt(9), rnd.nextInt(60));
                try {
                    Order pedido = crearPedido(productos, proveedores, rnd, cuando);
                    if (pedido == null) {
                        fallidos++;
                        continue;
                    }
                    orderService.confirm(pedido.getId(), RUTH_DNI);
                    pendientes.add(new Backdate(pedido.getId(), cuando, cuando.plusMinutes(5 + rnd.nextInt(120))));
                    total++;
                } catch (RuntimeException e) {
                    // Sin stock del talle elegido (confirm es estricto): se saltea.
                    fallidos++;
                }
            }
            // Reposición a mitad de camino: mantiene el stock con vida y genera
            // movimientos ENTRADA_COMPRA + recalculo de costo promedio ponderado.
            if (m % 2 == 0) reponer(productos, proveedores, rnd, 12);
        }

        if (fallidos > 0) {
            log.info("Seeder de demo: {} pedidos salteados por falta de stock.", fallidos);
        }
        return total;
    }

    private Order crearPedido(List<Product> productos, List<Supplier> proveedores, Random rnd,
                             LocalDateTime cuando) {
        boolean local = rnd.nextInt(100) < 55; // 55% local, 45% web
        String cliente = CLIENTES[rnd.nextInt(CLIENTES.length)];
        String email = slug(cliente) + "@" + EMAIL_DOMAIN;

        List<OrderDtos.CartItem> items = new ArrayList<>();
        int lineas = 1 + rnd.nextInt(3);
        for (int i = 0; i < lineas; i++) {
            Product p = productos.get(rnd.nextInt(productos.size()));
            int qty = 1 + rnd.nextInt(2);
            // El stock en memoria queda viejo a medida que se confirman pedidos,
            // así que el talle se elige contra la base (stockOf), no contra el
            // snapshot: si no, mitad de los pedidos fallarían por falta de stock.
            List<SizeStock> talles = new ArrayList<>(p.getSizeStocks());
            Collections.shuffle(talles, rnd);
            String elegido = null;
            for (SizeStock s : talles) {
                if (productService.stockOf(p.getId(), s.getSize()) >= qty) {
                    elegido = s.getSize();
                    break;
                }
            }
            if (elegido == null) continue;
            items.add(new OrderDtos.CartItem(p.getId(), elegido, qty));
        }
        if (items.isEmpty()) return null; // sin nada que vender: se saltea el pedido

        PaymentMethod pago = local ? pagoLocal(rnd) : pagoWeb(rnd);
        DeliveryMethod entrega = (!local && rnd.nextInt(100) < 25)
                ? DeliveryMethod.SHIPPING : DeliveryMethod.PICKUP;

        String direccion = null;
        String referencia = null;
        if (entrega == DeliveryMethod.SHIPPING) {
            direccion = CALLES[rnd.nextInt(CALLES.length)] + " " + (100 + rnd.nextInt(1800))
                    + ", San Miguel de Tucumán";
            referencia = "Casa " + (rnd.nextBoolean() ? "con reja" : "de dos plantas");
        }

        OrderDtos.CreateOrderRequest req = new OrderDtos.CreateOrderRequest(
                cliente, email, items, entrega, direccion, referencia,
                null, null, pago, null);

        return local ? orderService.createPos(req, RUTH_DNI) : orderService.create(req);
    }

    private PaymentMethod pagoLocal(Random rnd) {
        return switch (rnd.nextInt(4)) {
            case 0 -> PaymentMethod.CASH;
            case 1 -> PaymentMethod.TRANSFER;
            case 2 -> PaymentMethod.QR_TRANSFER;
            default -> PaymentMethod.QR_CARD;
        };
    }

    private PaymentMethod pagoWeb(Random rnd) {
        return switch (rnd.nextInt(3)) {
            case 0 -> PaymentMethod.TRANSFER;
            case 1 -> PaymentMethod.QR_TRANSFER;
            default -> PaymentMethod.QR_CARD;
        };
    }

    /**
     * Compra a proveedor de talles que estén bajos, para reponer stock. Intenta
     * {@code intentos} veces (el talle se elige contra la base con
     * {@code stockOf}, no contra el snapshot en memoria).
     */
    private void reponer(List<Product> productos, List<Supplier> proveedores, Random rnd, int intentos) {
        for (int i = 0; i < intentos; i++) {
            Product p = productos.get(rnd.nextInt(productos.size()));
            List<SizeStock> flojos = p.getSizeStocks().stream()
                    .filter(s -> productService.stockOf(p.getId(), s.getSize()) <= 2)
                    .toList();
            if (flojos.isEmpty()) continue;
            SizeStock s = flojos.get(rnd.nextInt(flojos.size()));
            BigDecimal costo = (p.getCostPrice() != null ? p.getCostPrice()
                    : p.getPrice().multiply(BigDecimal.valueOf(0.6)))
                    .setScale(2, RoundingMode.HALF_UP);
            String provId = proveedores.isEmpty() ? null
                    : proveedores.get(rnd.nextInt(proveedores.size())).getId();
            try {
                productService.registerPurchase(p.getId(), s.getSize(), 6 + rnd.nextInt(18), costo, provId, RUTH_DNI);
            } catch (RuntimeException ignored) {
                // producto borrado a mitad de camino: se ignora
            }
        }
    }

    // ------------------------------------------------------------------
    //  Turnos, cambios y campañas
    // ------------------------------------------------------------------

    private void seedTurnos() {
        // Un turno de hoy ya cerrado y uno abierto, para que /admin/turnos y la
        // caja tengan qué mostrar sin dejar basura rara.
        var abierto = shiftService.open(RUTH_DNI);
        shiftService.close(abierto.getId(), RUTH_DNI);
        shiftService.open(RUTH_DNI);
    }

    private void seedCambios(List<Product> productos, Random rnd) {
        for (int i = 0; i < 8; i++) {
            Product devuelve = productos.get(rnd.nextInt(productos.size()));
            Product seLleva = productos.get(rnd.nextInt(productos.size()));
            String talleDevuelve = devuelve.getSizeStocks().get(0).getSize();
            String talleSeLleva = seLleva.getSizeStocks().get(0).getSize();
            try {
                exchangeService.create(new ExchangeDtos.CreateExchangeRequest(
                        CLIENTES[rnd.nextInt(CLIENTES.length)],
                        List.of(new ExchangeDtos.ExchangeItem(devuelve.getId(), talleDevuelve, 1)),
                        List.of(new ExchangeDtos.ExchangeItem(seLleva.getId(), talleSeLleva, 1)),
                        pagoLocal(rnd),
                        "Cambio de talle (demo)"), RUTH_DNI);
            } catch (RuntimeException ignored) {
                // sin stock para lo que se lleva: se saltea
            }
        }
    }

    /** Historial de campañas, para que /admin/campanias tenga filas y el cooldown tenga sentido. */
    private void seedCampanias(Random rnd) {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 60; i++) {
            String cliente = CLIENTES[rnd.nextInt(CLIENTES.length)];
            MarketingSend m = new MarketingSend();
            m.setId(java.util.UUID.randomUUID().toString());
            m.setEmail(slug(cliente) + "@" + EMAIL_DOMAIN);
            m.setReason(rnd.nextInt(100) < 60 ? MarketingSend.Reason.INACTIVE : MarketingSend.Reason.VIP);
            m.setStatus(rnd.nextInt(100) < 92 ? MarketingSend.Status.SENT : MarketingSend.Status.FAILED);
            if (m.getStatus() == MarketingSend.Status.SENT) {
                m.setCouponCode("DEMO-" + (1000 + rnd.nextInt(9000)));
            } else {
                m.setErrorMessage("Mailbox full (demo)");
            }
            m.setLifetimeSpendSnapshot(BigDecimal.valueOf(50000 + rnd.nextInt(400000)));
            LocalDateTime cuando = hoy.minusDays(rnd.nextInt(300)).atTime(6, rnd.nextInt(60));
            m.setSentAt(cuando.atZone(ZoneId.systemDefault()).toInstant());
            m.setLastOrderAtSnapshot(Instant.now().minusSeconds(rnd.nextInt(86400 * 200)));
            marketingSendRepo.save(m);
        }
    }

    // ------------------------------------------------------------------

    private static String slug(String nombre) {
        return java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z]+", ".");
    }
}
