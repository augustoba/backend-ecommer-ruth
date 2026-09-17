package com.saasweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasweb.common.TenantContext;
import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderService;
import com.saasweb.core.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cubre {@link OrderService#confirmFromPayment}, el punto que arregla el
 * aviso de "pago aprobado por Mercado Pago pero no se pudo confirmar solo"
 * (antes quedaba sólo en un log del servidor, invisible para la tienda). No
 * pasa por el webhook real (ese sí necesita la API de Mercado Pago) — llama
 * al service directo, que es la parte que de verdad cambia el estado del
 * pedido.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderMercadoPagoConfirmTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    OrderService orderService;
    @Autowired
    TenantService tenantService;

    /**
     * {@code confirmFromPayment} normalmente lo llama el webhook dentro de
     * una request HTTP, con {@code TenantContext} ya seteado por
     * {@code TenantResolutionFilter}. Llamado directo desde el test (fuera
     * de una request), hay que setearlo a mano para que encuentre el
     * pedido — mismo tenant que usó el MockMvc para crearlo.
     */
    private Order confirmFromPaymentAsIfFromWebhook(String orderId, String mpPaymentId) {
        TenantContext.set(tenantService.resolveCurrentTenantId());
        try {
            return orderService.confirmFromPayment(orderId, mpPaymentId);
        } finally {
            TenantContext.clear();
        }
    }

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"test-pass\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private String createProduct(String jwt, int stock) throws Exception {
        String scaleId = mapper.readTree(
                        mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        String create = "{\"name\":\"MP test\",\"description\":\"prueba\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":" + stock + "}]}";
        return mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private String createOrder(String productId, int quantity) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"RN\",\"quantity\":" + quantity + "}]}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void keepsPaymentApprovedAndLeavesAVisibleNoteWhenStockIsNotEnough() throws Exception {
        String jwt = token();
        String productId = createProduct(jwt, 1);
        String orderId = createOrder(productId, 3);

        Order result = confirmFromPaymentAsIfFromWebhook(orderId, "fake-mp-payment-id");

        // el pago quedó marcado como aprobado (el cliente pagó de verdad)...
        assertThat(result.getPaymentStatus().name()).isEqualTo("APPROVED");
        assertThat(result.getMpPaymentId()).isEqualTo("fake-mp-payment-id");
        // ...pero el pedido NO se confirmó solo, y queda un aviso legible
        assertThat(result.getStatus().name()).isEqualTo("PENDIENTE");
        assertThat(result.getPaymentIssueNote()).contains("Mercado Pago aprobó el pago");

        // y el stock no se tocó
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products/" + productId))
                .andReturn().getResponse().getContentAsString());
        assertThat(product.get("sizeStocks").get(0).get("stock").asInt()).isEqualTo(1);
    }

    @Test
    void confirmsNormallyAndLeavesNoNoteWhenStockIsEnough() throws Exception {
        String jwt = token();
        String productId = createProduct(jwt, 5);
        String orderId = createOrder(productId, 2);

        Order result = confirmFromPaymentAsIfFromWebhook(orderId, "fake-mp-payment-id-2");

        assertThat(result.getPaymentStatus().name()).isEqualTo("APPROVED");
        assertThat(result.getStatus().name()).isEqualTo("PROCESADO");
        assertThat(result.getPaymentIssueNote()).isNull();

        JsonNode product = mapper.readTree(mvc.perform(get("/api/products/" + productId))
                .andReturn().getResponse().getContentAsString());
        assertThat(product.get("sizeStocks").get(0).get("stock").asInt()).isEqualTo(3);
    }
}
