package com.saasweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Historial de movimientos de stock (ítems 7/8) y costeo por promedio ponderado (ítems 9/18). */
@SpringBootTest
@AutoConfigureMockMvc
class StockMovementTest {

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

    private String scaleId() throws Exception {
        return mapper.readTree(
                mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
    }

    @Test
    void confirmingAnOrderLogsAVentaMovement() throws Exception {
        String jwt = token();
        String create = "{\"name\":\"Movimiento venta\",\"description\":\"prueba de movimiento\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId() + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":5}]}";
        String productId = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String orderId = mapper.readTree(mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"RN\",\"quantity\":2}]}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        String movements = mvc.perform(get("/api/admin/stock-movements?productId=" + productId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = mapper.readTree(movements);
        // ALTA_INICIAL (+5) y VENTA (-2)
        assert arr.size() == 2;
        boolean foundVenta = false;
        for (JsonNode m : arr) {
            if (m.get("reason").asText().equals("VENTA")) {
                assert m.get("quantityDelta").asInt() == -2;
                assert m.get("referenceId").asText().equals(orderId);
                foundVenta = true;
            }
        }
        assert foundVenta;
    }

    @Test
    void manualAdjustmentLogsAnAjusteManualMovementWithNote() throws Exception {
        String jwt = token();
        String create = "{\"name\":\"Movimiento ajuste\",\"description\":\"prueba de ajuste manual\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId() + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":5}]}";
        String productId = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(patch("/api/admin/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":\"RN\",\"stock\":2,\"note\":\"Se rompieron 3\"}"))
                .andExpect(status().isOk());

        String movements = mvc.perform(get("/api/admin/stock-movements?productId=" + productId)
                        .header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = mapper.readTree(movements);
        boolean found = false;
        for (JsonNode m : arr) {
            if (m.get("reason").asText().equals("AJUSTE_MANUAL")) {
                assert m.get("quantityDelta").asInt() == -3;
                assert m.get("note").asText().equals("Se rompieron 3");
                found = true;
            }
        }
        assert found;
    }

    @Test
    void purchaseRecalculatesWeightedAverageCost() throws Exception {
        String jwt = token();
        // 10 unidades a costo $500
        String create = "{\"name\":\"Costeo promedio\",\"description\":\"prueba de promedio ponderado\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId() + "\","
                + "\"params\":{},\"costPrice\":500,\"sizeStocks\":[{\"size\":\"RN\",\"stock\":10}]}";
        String productId = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // compra de 10 unidades más a $700 -> promedio (10*500 + 10*700) / 20 = 600
        String resp = mvc.perform(post("/api/admin/products/" + productId + "/purchases")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":\"RN\",\"quantity\":10,\"unitCost\":700}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode product = mapper.readTree(resp);
        assert product.get("costPrice").asDouble() == 600.0;
        assert product.get("sizeStocks").get(0).get("stock").asInt() == 20;

        String movements = mvc.perform(get("/api/admin/stock-movements?productId=" + productId)
                        .header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = mapper.readTree(movements);
        boolean found = false;
        for (JsonNode m : arr) {
            if (m.get("reason").asText().equals("ENTRADA_COMPRA")) {
                assert m.get("quantityDelta").asInt() == 10;
                assert m.get("unitCost").asDouble() == 700.0;
                found = true;
            }
        }
        assert found;
    }
}
