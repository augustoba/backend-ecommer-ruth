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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFilterTest {

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

    private String seedOrder(String customer) throws Exception {
        JsonNode products = mapper.readTree(
                mvc.perform(get("/api/products")).andReturn().getResponse().getContentAsString());
        String pid = products.get(0).get("id").asText();
        String size = products.get(0).get("sizeStocks").get(0).get("size").asText();
        return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"" + customer + "\",\"items\":[{\"productId\":\"" + pid
                                + "\",\"size\":\"" + size + "\",\"quantity\":1}]}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void filtersByNameCodeStatusAndDate() throws Exception {
        String jwt = token();
        String created = seedOrder("Filtro Fulanito");
        JsonNode order = mapper.readTree(created);
        String code = order.get("code").asText();          // PED-XXXX
        int number = Integer.parseInt(code.substring(4));   // XXXX
        String today = LocalDate.now().toString();
        String futureDay = LocalDate.now().plusDays(1).toString();

        // por nombre
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + jwt).param("search", "fulanito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerName").value("Filtro Fulanito"));

        // por número de pedido (con y sin prefijo)
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + jwt).param("search", code))
                .andExpect(jsonPath("$.content[0].code").value(code));
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + jwt)
                        .param("search", String.valueOf(number)))
                .andExpect(jsonPath("$.content[0].code").value(code));

        // por estado (recién creado → PENDIENTE; CANCELADO no debería traerlo)
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + jwt)
                        .param("search", "fulanito").param("status", "CANCELADO"))
                .andExpect(jsonPath("$.content.length()").value(0));

        // por fecha: hoy sí, mañana no
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + jwt)
                        .param("search", "fulanito").param("from", today).param("to", today))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + jwt)
                        .param("search", "fulanito").param("from", futureDay))
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
