package com.estilospequenos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderConfirmTest {

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
    void confirmIsRejectedWhenStockIsNotEnough() throws Exception {
        String jwt = token();

        // producto nuevo con 1 sola unidad en un talle
        String scaleId = mapper.readTree(
                mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        String create = "{\"name\":\"Stock corto\",\"description\":\"prueba de sobreventa\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":1}]}";
        String productId = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // pedido por 3 unidades
        String orderId = mapper.readTree(mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"RN\",\"quantity\":3}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // confirmar → 400, y el pedido sigue PENDIENTE
        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("stock")));

        mvc.perform(get("/api/admin/orders/" + orderId).header("Authorization", "Bearer " + jwt))
                .andExpect(jsonPath("$.status").value("PENDIENTE"));

        // el stock del producto no se tocó
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products/" + productId))
                .andReturn().getResponse().getContentAsString());
        assert product.get("sizeStocks").get(0).get("stock").asInt() == 1;
    }

    @Test
    void confirmSucceedsWhenStockIsEnough() throws Exception {
        String jwt = token();
        String scaleId = mapper.readTree(
                mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        String create = "{\"name\":\"Stock ok\",\"description\":\"prueba de confirmacion\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId + "\","
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

        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESADO"));

        JsonNode product = mapper.readTree(mvc.perform(get("/api/products/" + productId))
                .andReturn().getResponse().getContentAsString());
        assert product.get("sizeStocks").get(0).get("stock").asInt() == 3;
    }
}
