package com.estilospequenos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicCatalogIsSeeded() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].params").exists())
                .andExpect(jsonPath("$[0].sizeStocks").isArray())
                .andExpect(jsonPath("$[0].images").isArray())
                .andExpect(jsonPath("$[0].images.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].imageUrl").isNotEmpty());
    }

    @Test
    void paramGroupsAndSizeScalesArePublic() throws Exception {
        mvc.perform(get("/api/param-groups")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)));
        mvc.perform(get("/api/size-scales")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)));
    }

    @Test
    void siteSettingsArePublicAndSeeded() throws Exception {
        mvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").isNotEmpty())
                .andExpect(jsonPath("$.whatsappNumber").isNotEmpty());
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatesProductWithGallery() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String login = mvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test-pass\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).get("token").asText();

        String body = "{\"name\":\"Remera test\",\"description\":\"Descripcion de prueba\","
                + "\"price\":9999,\"ageRange\":\"2 a 4\",\"images\":[\"https://x/a.jpg\",\"https://x/b.jpg\"],"
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"2\",\"stock\":3}]}";

        mvc.perform(post("/api/admin/products").header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.imageUrl").value("https://x/a.jpg"));

        // sin imágenes → 400
        mvc.perform(post("/api/admin/products").header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body.replace("[\"https://x/a.jpg\",\"https://x/b.jpg\"]", "[]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderCalculatesTotals() throws Exception {
        // el primer producto sembrado; tomamos su id y un talle
        String products = mvc.perform(get("/api/products"))
                .andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode arr =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(products);
        String productId = arr.get(0).get("id").asText();
        String size = arr.get(0).get("sizeStocks").get(0).get("size").asText();

        String payload = "{\"customerName\":\"Test\",\"items\":[{\"productId\":\""
                + productId + "\",\"size\":\"" + size + "\",\"quantity\":2}]}";

        mvc.perform(post("/api/orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("PED-")))
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").exists());
    }
}
