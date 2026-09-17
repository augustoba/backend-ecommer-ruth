package com.saasweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Alícuota de IVA por producto (ítem 4) y guardas de Notas de Crédito (ítem 2). */
@SpringBootTest
@AutoConfigureMockMvc
class ArcaInvoicingTest {

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
    void productIvaRatePersists() throws Exception {
        String jwt = token();
        String create = "{\"name\":\"Producto con IVA\",\"description\":\"prueba de alicuota\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId() + "\","
                + "\"params\":{},\"ivaRate\":10.5,\"sizeStocks\":[{\"size\":\"RN\",\"stock\":1}]}";
        JsonNode product = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assert product.get("ivaRate").asDouble() == 10.5;
    }

    @Test
    void creditNoteIsRejectedWhenOrderHasNoApprovedInvoice() throws Exception {
        String jwt = token();
        String create = "{\"name\":\"Producto sin factura\",\"description\":\"prueba de nota de credito\",\"price\":1000,"
                + "\"ageRange\":\"1 a 2\",\"images\":[\"https://x/a.jpg\"],\"sizeScaleId\":\"" + scaleId() + "\","
                + "\"params\":{},\"sizeStocks\":[{\"size\":\"RN\",\"stock\":5}]}";
        String productId = mapper.readTree(mvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String orderId = mapper.readTree(mvc.perform(post("/api/admin/orders/pos")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"size\":\"RN\",\"quantity\":1}],\"paymentMethod\":\"CASH\"}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceType").value("TICKET_INTERNO"));

        // Sin ARCA configurado (ni módulo del plan), queda en ticket interno -> no se puede emitir NC.
        mvc.perform(post("/api/admin/orders/" + orderId + "/credit-notes")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"reason\":\"Devolucion parcial\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Factura ARCA")));
    }
}
