package com.estilospequenos;

import com.estilospequenos.model.AdminUser;
import com.estilospequenos.repository.AdminUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    AdminUserRepository adminUsers;

    @Test
    void adminUserIsSeededWithBcryptHash() {
        AdminUser admin = adminUsers.findByDni("11111111").orElseThrow();
        assertThat(admin.getPasswordHash()).startsWith("$2");           // BCrypt
        assertThat(admin.getPasswordHash()).isNotEqualTo("test-pass");  // no en texto plano
    }

    @Test
    void adminEndpointRequiresToken() throws Exception {
        mvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginThenAccessAdmin() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"test-pass\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = mapper.readTree(body);
        String token = json.get("token").asText();

        mvc.perform(get("/api/admin/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * DNI que no existe: responde igual (204, sin revelar nada) y no intenta
     * mandar ningún mail (evita depender de una conexión SMTP real en tests).
     */
    @Test
    void forgotPasswordForUnknownDniReturnsNoContent() throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"99999999\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePasswordFlow() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"test-pass\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(body).get("token").asText();

        // con la pass actual mal → 401
        mvc.perform(put("/api/admin/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"xxx\",\"newPassword\":\"otra1234\"}"))
                .andExpect(status().isUnauthorized());

        // cambio real, y lo vuelvo a dejar como estaba para no romper otros tests
        mvc.perform(put("/api/admin/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"test-pass\",\"newPassword\":\"temporal99\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"11111111\",\"password\":\"temporal99\"}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/admin/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"temporal99\",\"newPassword\":\"test-pass\"}"))
                .andExpect(status().isNoContent());
    }
}
