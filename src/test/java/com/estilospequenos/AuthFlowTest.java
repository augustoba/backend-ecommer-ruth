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
        AdminUser admin = adminUsers.findByUsername("admin").orElseThrow();
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
                        .content("{\"username\":\"admin\",\"password\":\"test-pass\"}"))
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
                        .content("{\"username\":\"admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRecoveryPhraseIsRejected() throws Exception {
        mvc.perform(post("/api/auth/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"recoveryPhrase\":\"mal\",\"newPassword\":\"nueva123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recoverThenChangePasswordFlow() throws Exception {
        // 1) recuperar con la frase por defecto → nueva pass + token
        String recBody = mvc.perform(post("/api/auth/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"recoveryPhrase\":\"frase-de-recuperacion-cambiar\",\"newPassword\":\"recuperada9\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(recBody).get("token").asText();

        // 2) login con la pass nueva funciona
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"recuperada9\"}"))
                .andExpect(status().isOk());

        // 3) cambiar la pass (y dejarla como estaba para no romper otros tests)
        mvc.perform(put("/api/admin/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"recuperada9\",\"newPassword\":\"test-pass\"}"))
                .andExpect(status().isNoContent());

        // 4) con la pass actual mal → 401
        mvc.perform(put("/api/admin/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"xxx\",\"newPassword\":\"otra1234\"}"))
                .andExpect(status().isUnauthorized());
    }
}
