package com.estilospequenos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiscountDateTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test-pass\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    @Test
    void expiredDiscountIsNotAppliedAndIsMarkedVencido() throws Exception {
        String jwt = token();
        String yesterday = LocalDate.now().minusDays(1).toString();

        // producto barato
        String scaleId = mapper.readTree(mvc.perform(get("/api/size-scales"))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
        String productId = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Prod desc\",\"description\":\"prueba fechas\",\"price\":1000,"
                                + "\"ageRange\":\"1\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId
                                + "\",\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":10}]}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // descuento por monto 30%, mínimo $1, vencido ayer
        String discountId = mapper.readTree(mvc.perform(post("/api/admin/discounts")
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"MONTO\",\"discountPercent\":30,\"minAmount\":1,\"endsAt\":\""
                                + yesterday + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VENCIDO"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // el pedido no lleva ese descuento
        String order = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"RN\",\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode o = mapper.readTree(order);
        assert o.get("discountAmount").asInt() == 0 : "no debería aplicar un descuento vencido";

        // sin fecha de fin → ACTIVO y ahora sí aplica
        mvc.perform(put("/api/admin/discounts/" + discountId)
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"MONTO\",\"discountPercent\":30,\"minAmount\":1}"))
                .andExpect(jsonPath("$.status").value("ACTIVO"));

        JsonNode o2 = mapper.readTree(mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"RN\",\"quantity\":2}]}"))
                .andReturn().getResponse().getContentAsString());
        assert o2.get("discountAmount").asInt() > 0 : "sin vencimiento debería aplicar";
    }

    @Test
    void invalidRangeIsRejected() throws Exception {
        String jwt = token();
        mvc.perform(post("/api/admin/discounts")
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"MONTO\",\"discountPercent\":10,\"minAmount\":1,"
                                + "\"startsAt\":\"2026-12-01\",\"endsAt\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }
}
