package com.axel20378.heat_exchanger_selector.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет ключевые сценарии авторизации и прав доступа (задачи Тиграна):
 *  - анонимный доступ к защищенным ресурсам запрещен;
 *  - обычный пользователь может зарегистрироваться, войти и получить свой профиль;
 *  - обычный пользователь НЕ может попасть в административные функции;
 *  - администратор (создан автоматически при старте) имеет доступ к административным функциям.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void anonymousRequestToProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanRegisterLoginAndReadOwnProfile() throws Exception {
        register("ivan", "password123", "ivan@example.com");

        MockHttpSession session = login("ivan", "password123");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ivan"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void regularUserIsForbiddenFromAdminEndpoints() throws Exception {
        register("petr", "password123", "petr@example.com");
        MockHttpSession session = login("petr", "password123");

        mockMvc.perform(get("/api/admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void seededAdminCanAccessAdminEndpoints() throws Exception {
        // Учетная запись администратора создается автоматически при старте приложения
        // (см. AdminAccountInitializer / application-test.yaml).
        MockHttpSession session = login("admin", "admin12345");

        mockMvc.perform(get("/api/admin/users").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register("olga", "password123", "olga@example.com");

        String body = objectMapper.writeValueAsString(new LoginPayload("olga", "wrong-password"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    private void register(String username, String password, String email) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload(username, password, email));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String body = objectMapper.writeValueAsString(new LoginPayload(username, password));
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return session;
    }

    private record RegisterPayload(String username, String password, String email) {
    }

    private record LoginPayload(String username, String password) {
    }
}
