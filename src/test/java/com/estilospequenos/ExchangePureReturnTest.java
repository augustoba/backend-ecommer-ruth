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

/** Cambios: devolución pura (el cliente devuelve algo y no se lleva nada a cambio). */
@SpringBootTest
@AutoConfigureMockMvc
class ExchangePureReturnTest {

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

    private String createProduct(String jwt, String name, int stock) throws Exception {
        String scaleId = mapper.readTree(
                mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        String create = "{\"name\":\"" + name + "\",\"description\":\"prueba devolucion pura\",\"price\":1000,"
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
    void pureReturnWithoutTakenItemsRestocksAndRefunds() throws Exception {
        String jwt = token();
        String product = createProduct(jwt, "Devolucion Pura", 3);

        String body = "{\"customerName\":\"Sin cambio\","
                + "\"returned\":[{\"productId\":\"" + product + "\",\"size\":\"RN\",\"quantity\":1}],"
                + "\"paymentMethod\":\"TRANSFER\"}";

        mvc.perform(post("/api/admin/exchanges")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnedTotal").value(1000))
                .andExpect(jsonPath("$.takenTotal").value(0))
                .andExpect(jsonPath("$.difference").value(-1000))
                .andExpect(jsonPath("$.paymentMethod").value("TRANSFER"))
                .andExpect(jsonPath("$.lines.length()").value(1));

        assert stockOf(product) == 4; // 3 + 1 devuelto, nada se llevó
    }
}
