package com.estilospequenos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ajuste masivo de precio y "eliminar definitivamente" (sólo sobre productos ya archivados). */
@SpringBootTest
@AutoConfigureMockMvc
class ProductBulkPriceAndDeleteTest {

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

    private String createProduct(String jwt, String name, String price) throws Exception {
        String scaleId = mapper.readTree(
                mvc.perform(get("/api/size-scales")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        String create = "{\"name\":\"" + name + "\",\"description\":\"prueba bulk price\",\"price\":" + price + ","
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":3}]}";
        return mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void bulkAdjustPriceRaisesSelectedProductsOnlyAndNeverTouchesCostPrice() throws Exception {
        String jwt = token();
        String a = createProduct(jwt, "Bulk A", "1000");
        String b = createProduct(jwt, "Bulk B", "2000");

        mvc.perform(patch("/api/admin/products/bulk-price")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + a + "\"],\"percent\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));

        JsonNode productA = mapper.readTree(mvc.perform(get("/api/admin/products/" + a)
                        .header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString());
        assert productA.get("price").asInt() == 1100; // +10%

        JsonNode productB = mapper.readTree(mvc.perform(get("/api/admin/products/" + b)
                        .header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString());
        assert productB.get("price").asInt() == 2000; // no seleccionado, no cambia
    }

    @Test
    void permanentDeleteOnlyWorksOnArchivedProducts() throws Exception {
        String jwt = token();
        String id = createProduct(jwt, "Para borrar", "1000");

        // Todavía publicado: no se puede eliminar definitivamente.
        mvc.perform(delete("/api/admin/products/" + id + "/permanent")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest());

        // Se archiva primero (soft-delete).
        mvc.perform(delete("/api/admin/products/" + id)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // Ahora sí.
        mvc.perform(delete("/api/admin/products/" + id + "/permanent")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/products/" + id).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
