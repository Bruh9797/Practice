package com.axel20378.heat_exchanger_selector.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void csrfEndpointIssuesTokenAndUnsafeRequestRequiresIt() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("admin", "admin12345"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_MISSING"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void invalidCsrfTokenUsesStableErrorCode() throws Exception {
        CsrfCredentials csrf = csrf(null);

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("admin", "admin12345"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void anonymousRequestToProtectedEndpointUsesUnifiedError() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    void catalogImagesAreAvailableToTheBrowser() throws Exception {
        mockMvc.perform(get("/catalog-images/test.webp"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/webp"));
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
    void sessionIdentifierChangesDuringJsonLogin() throws Exception {
        register("session-user", "password123", "session@example.com");
        MockHttpSession session = new MockHttpSession();
        String oldSessionId = session.getId();

        login("session-user", "password123", session);

        assertThat(session.getId()).isNotEqualTo(oldSessionId);
    }

    @Test
    void regularUserIsForbiddenFromAdminEndpoints() throws Exception {
        register("petr", "password123", "petr@example.com");
        MockHttpSession session = login("petr", "password123");

        mockMvc.perform(get("/api/admin/users").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void seededAdminCanAccessAdminEndpoints() throws Exception {
        MockHttpSession session = login("admin", "admin12345");

        mockMvc.perform(get("/api/admin/users").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUnknownApiUsesUnifiedNotFoundError() throws Exception {
        MockHttpSession session = login("admin", "admin12345");

        mockMvc.perform(get("/api/no-such-endpoint").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/no-such-endpoint"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register("olga", "password123", "olga@example.com");
        CsrfCredentials csrf = csrf(null);

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("olga", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register("first-email", "password123", "duplicate@example.com");
        CsrfCredentials csrf = csrf(null);

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterPayload("second-email", "password123", "DUPLICATE@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void registrationValidatesNormalizedUsernameLength() throws Exception {
        CsrfCredentials csrf = csrf(null);

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterPayload(" a ", "password123", "normalized@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void roleAndEnabledStateAreRefreshedFromDatabaseForExistingSession() throws Exception {
        register("fresh-user", "password123", "fresh@example.com");
        MockHttpSession session = login("fresh-user", "password123");

        User user = userRepository.findByUsername("fresh-user").orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/admin/users").session(session))
                .andExpect(status().isOk());

        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    private void register(String username, String password, String email) throws Exception {
        CsrfCredentials csrf = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload(username, password, email))))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return login(username, password, new MockHttpSession());
    }

    private MockHttpSession login(String username, String password, MockHttpSession session) throws Exception {
        CsrfCredentials csrf = csrf(session);
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(username, password))))
                .andExpect(status().isOk());
        return session;
    }

    private CsrfCredentials csrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) {
            request.session(session);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        return new CsrfCredentials(body.get("headerName").asString(), body.get("token").asString(), cookie);
    }

    private record CsrfCredentials(String headerName, String token, Cookie cookie) {
    }

    private record RegisterPayload(String username, String password, String email) {
    }

    private record LoginPayload(String username, String password) {
    }
}
