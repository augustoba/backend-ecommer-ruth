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
    void adminEditsLogoAndWhatsappTexts() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        // La config de plataforma es sólo-superadmin: logueamos con esa cuenta, no con la de Ruth.
        String login = mvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"33756194\",\"password\":\"augusto123\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).get("token").asText();

        String body = "{\"storeName\":\"Estilos Pequeños\",\"whatsappNumber\":\"5491122334455\","
                + "\"logoUrl\":\"data:image/png;base64,ABC123\","
                + "\"whatsappIntro\":\"Hola desde {tienda}\",\"whatsappClosing\":\"Pagá al alias mi.alias\"}";

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/settings/platform").header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("data:image/png;base64,ABC123"))
                .andExpect(jsonPath("$.whatsappIntro").value("Hola desde {tienda}"))
                .andExpect(jsonPath("$.whatsappClosing").value("Pagá al alias mi.alias"));

        // vacío en el request → vuelve a null (usa el default en el front)
        String cleared = "{\"storeName\":\"Estilos Pequeños\",\"whatsappNumber\":\"5491122334455\","
                + "\"logoUrl\":\"\",\"whatsappIntro\":\"\",\"whatsappClosing\":\"  \"}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/settings/platform").header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cleared))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.whatsappIntro").value(org.hamcrest.Matchers.nullValue()));

        // dejar la fila singleton como estaba (otros tests de la clase la leen)
        String restore = "{\"storeName\":\"Estilos Pequeños\",\"whatsappNumber\":\"5491122334455\","
                + "\"aboutText\":\"Somos Estilos Pequeños.\",\"instagram\":\"estilospequenos_\","
                + "\"facebookUrl\":\"https://www.facebook.com/share/1NZXdYgick/\"}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/settings/platform").header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(restore))
                .andExpect(status().isOk());
    }

    @Test
    void adminCreatesProductWithGallery() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String login = mvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"test-pass\"}"))
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
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.deliveryMethod").value("PICKUP"));
    }

    @Test
    void createOrderWithShippingAndPayment() throws Exception {
        String products = mvc.perform(get("/api/products")).andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode arr =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(products);
        String productId = arr.get(0).get("id").asText();
        String size = arr.get(0).get("sizeStocks").get(0).get("size").asText();
        String items = "\"items\":[{\"productId\":\"" + productId + "\",\"size\":\"" + size + "\",\"quantity\":1}]";

        // envío sin dirección → 400
        mvc.perform(post("/api/orders").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Test\"," + items + ",\"deliveryMethod\":\"SHIPPING\"}"))
                .andExpect(status().isBadRequest());

        // envío con dirección + pago → OK y se guardan los datos
        String ok = "{\"customerName\":\"Test\"," + items + ",\"deliveryMethod\":\"SHIPPING\","
                + "\"shippingAddress\":\"San Martín 500, San Miguel de Tucumán\","
                + "\"shippingReference\":\"depto 3B\",\"shippingLat\":-26.83,\"shippingLng\":-65.20,"
                + "\"paymentMethod\":\"TRANSFER\"}";
        mvc.perform(post("/api/orders").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryMethod").value("SHIPPING"))
                .andExpect(jsonPath("$.shippingAddress").value("San Martín 500, San Miguel de Tucumán"))
                .andExpect(jsonPath("$.shippingLat").value(-26.83))
                .andExpect(jsonPath("$.paymentMethod").value("TRANSFER"));
    }
}
