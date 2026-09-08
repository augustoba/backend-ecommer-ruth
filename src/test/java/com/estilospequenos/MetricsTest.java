package com.estilospequenos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetricsTest {

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
    void metricsRequireToken() throws Exception {
        mvc.perform(get("/api/admin/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRangeIsRejected() throws Exception {
        mvc.perform(get("/api/admin/metrics")
                        .header("Authorization", "Bearer " + token())
                        .param("from", "2026-12-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processedOrderShowsUpInMetrics() throws Exception {
        String jwt = token();

        JsonNode products = mapper.readTree(
                mvc.perform(get("/api/products")).andReturn().getResponse().getContentAsString());
        String productId = products.get(0).get("id").asText();
        String size = products.get(0).get("sizeStocks").get(0).get("size").asText();

        String created = mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Metrics\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"" + size + "\",\"quantity\":3}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = mapper.readTree(created).get("id").asText();

        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/metrics").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.units", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totals.orders", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.byMonth").isArray())
                .andExpect(jsonPath("$.byGroup.groupId").value("grp-tipo"))
                .andExpect(jsonPath("$.topProducts[0].units", greaterThanOrEqualTo(3)));

        mvc.perform(get("/api/admin/metrics/comparison").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").isNumber())
                .andExpect(jsonPath("$.week.dayFrom").isNumber())
                .andExpect(jsonPath("$.monthly").isArray())
                .andExpect(jsonPath("$.weekly").isArray());
    }

    @Test
    void comparisonRequiresToken() throws Exception {
        mvc.perform(get("/api/admin/metrics/comparison")).andExpect(status().isUnauthorized());
    }
}
