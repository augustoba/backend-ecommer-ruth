package com.estilospequenos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardTest {

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
    void dashboardRequiresToken() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardHasSummaryAndLowStock() throws Exception {
        String jwt = token();
        String body = mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingOrders").exists())
                .andExpect(jsonPath("$.month.revenue").exists())
                .andExpect(jsonPath("$.products.active").exists())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.defaultLowStockThreshold").value(3))
                .andExpect(jsonPath("$.lowStock").isArray())
                .andReturn().getResponse().getContentAsString();

        // toda entrada de lowStock respeta stock <= threshold
        JsonNode low = mapper.readTree(body).get("lowStock");
        for (JsonNode item : low) {
            assert item.get("stock").asInt() <= item.get("threshold").asInt();
        }

        mvc.perform(get("/api/admin/low-stock").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void discontinuedProductDropsOutOfLowStock() throws Exception {
        String jwt = token();

        String body = "{\"name\":\"Prenda a discontinuar\",\"description\":\"Descripcion de prueba\","
                + "\"price\":9999,\"ageRange\":\"2 a 4\",\"images\":[\"https://x/a.jpg\"],"
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"2\",\"stock\":1}]}";
        String created = mvc.perform(post("/api/admin/products").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(created).get("id").asText();

        // stock 1 <= 3 → aparece en la lista de reposición
        String before = mvc.perform(get("/api/admin/low-stock").header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString();
        assert before.contains(id);

        // marcar "no reponer"
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/products/" + id + "/discontinued")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"discontinued\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discontinued").value(true));

        // ya no aparece, aunque sigue con stock bajo y activo
        String after = mvc.perform(get("/api/admin/low-stock").header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString();
        assert !after.contains(id);
    }
}
