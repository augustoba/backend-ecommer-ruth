package com.estilospequenos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entrega/cancelación parcial de un pedido: el dueño puede resolver algunas
 * líneas (entregar/cancelar) mientras otras siguen pendientes, y editar el
 * pedido (agregar/cambiar cantidad/sacar ítems) mientras sigue pendiente.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderPartialLifecycleTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"test-pass\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    /** Crea un producto propio de este test (no comparte catálogo con otros tests). */
    private String createProduct(String jwt, String name, int stock) throws Exception {
        String scaleId = mapper.readTree(
                mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        String create = "{\"name\":\"" + name + "\",\"description\":\"prueba entrega parcial\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":" + stock + "}]}";
        return mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private int stockOf(String productId) throws Exception {
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products/" + productId))
                .andReturn().getResponse().getContentAsString());
        return product.get("sizeStocks").get(0).get("stock").asInt();
    }

    @Test
    void confirmLinesAndCancelLinesResolveIndependentlyAndOrderEndsProcesado() throws Exception {
        String jwt = token();
        String productA = createProduct(jwt, "Parcial A", 5);
        String productB = createProduct(jwt, "Parcial B", 5);

        String orderBody = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Parcial\",\"items\":["
                                + "{\"productId\":\"" + productA + "\",\"size\":\"RN\",\"quantity\":2},"
                                + "{\"productId\":\"" + productB + "\",\"size\":\"RN\",\"quantity\":3}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode order = mapper.readTree(orderBody);
        String orderId = order.get("id").asText();
        String lineA = order.get("lines").get(0).get("id").asText();
        String lineB = order.get("lines").get(1).get("id").asText();

        // Entrega sólo la línea A — la B sigue pendiente y el pedido no resuelve todavía.
        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm-lines")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineIds\":[\"" + lineA + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.lines[0].status").value("ENTREGADA"))
                .andExpect(jsonPath("$.lines[1].status").value("PENDIENTE"));

        assert stockOf(productA) == 3; // 5 - 2 entregadas
        assert stockOf(productB) == 5; // todavía no se tocó

        // No se puede volver a entregar/cancelar una línea ya entregada.
        mvc.perform(post("/api/admin/orders/" + orderId + "/cancel-lines")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineIds\":[\"" + lineA + "\"]}"))
                .andExpect(status().isBadRequest());

        // Cancela la línea B — con eso ya no queda ninguna pendiente, y como
        // A se entregó, el pedido pasa a PROCESADO (no CANCELADO).
        mvc.perform(post("/api/admin/orders/" + orderId + "/cancel-lines")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineIds\":[\"" + lineB + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESADO"))
                .andExpect(jsonPath("$.lines[1].status").value("CANCELADA"));

        assert stockOf(productB) == 5; // cancelada: nunca se le tocó el stock
    }

    @Test
    void allLinesCancelledEndsOrderCancelado() throws Exception {
        String jwt = token();
        String product = createProduct(jwt, "Parcial Cancel Total", 5);

        String orderBody = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Cancela todo\",\"items\":["
                                + "{\"productId\":\"" + product + "\",\"size\":\"RN\",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = mapper.readTree(orderBody).get("id").asText();

        mvc.perform(post("/api/admin/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assert stockOf(product) == 5;
    }

    @Test
    void addUpdateAndRemoveLineRecalculateTotalsOnAPendingOrder() throws Exception {
        String jwt = token();
        String productA = createProduct(jwt, "Editar A", 5);
        String productB = createProduct(jwt, "Editar B", 5);

        String orderBody = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Editar\",\"items\":["
                                + "{\"productId\":\"" + productA + "\",\"size\":\"RN\",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = mapper.readTree(orderBody);
        String orderId = created.get("id").asText();
        BigDecimal discount = new BigDecimal(created.get("discountAmount").asText());

        // Agrega un segundo ítem.
        String afterAdd = mvc.perform(post("/api/admin/orders/" + orderId + "/lines")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productB + "\",\"size\":\"RN\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode afterAddNode = mapper.readTree(afterAdd);
        assert afterAddNode.get("lines").size() == 2;
        BigDecimal expectedSubtotal = new BigDecimal("1000").add(new BigDecimal("2000")); // 1x1000 + 2x1000
        assert new BigDecimal(afterAddNode.get("subtotal").asText()).compareTo(expectedSubtotal) == 0;
        assert new BigDecimal(afterAddNode.get("total").asText())
                .compareTo(expectedSubtotal.subtract(discount)) == 0;

        String lineB = afterAddNode.get("lines").get(1).get("id").asText();

        // Cambia la cantidad del segundo ítem a 3.
        String afterQty = mvc.perform(patch("/api/admin/orders/" + orderId + "/lines/" + lineB)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode afterQtyNode = mapper.readTree(afterQty);
        BigDecimal subtotalAfterQty = new BigDecimal("1000").add(new BigDecimal("3000")); // 1x1000 + 3x1000
        assert new BigDecimal(afterQtyNode.get("subtotal").asText()).compareTo(subtotalAfterQty) == 0;

        // Saca el segundo ítem — vuelve a quedar sólo el original.
        String afterRemove = mvc.perform(delete("/api/admin/orders/" + orderId + "/lines/" + lineB)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode afterRemoveNode = mapper.readTree(afterRemove);
        assert afterRemoveNode.get("lines").size() == 1;
        assert new BigDecimal(afterRemoveNode.get("subtotal").asText()).compareTo(new BigDecimal("1000")) == 0;

        // No se puede sacar el último ítem que queda.
        String lastLine = afterRemoveNode.get("lines").get(0).get("id").asText();
        mvc.perform(delete("/api/admin/orders/" + orderId + "/lines/" + lastLine)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest());
    }
}
