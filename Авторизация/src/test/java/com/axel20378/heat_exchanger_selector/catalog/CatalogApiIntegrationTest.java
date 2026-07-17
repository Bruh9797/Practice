package com.axel20378.heat_exchanger_selector.catalog;

import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.StatusUpdate;
import com.axel20378.heat_exchanger_selector.catalog.repository.HeatExchangerRepository;
import com.axel20378.heat_exchanger_selector.security.Role;
import com.axel20378.heat_exchanger_selector.security.User;
import com.axel20378.heat_exchanger_selector.security.UserPrincipal;
import com.axel20378.heat_exchanger_selector.security.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HeatExchangerRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void anonymousUserCannotReadCatalog() throws Exception {
        mockMvc.perform(get("/api/heat-exchangers/lookups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanSearchButCannotUseAdminApi() throws Exception {
        UserPrincipal principal = principal(Role.USER, "catalog-user");
        mockMvc.perform(post("/api/heat-exchangers/search")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"families\":[\"AIR_COOLED\"],\"page\":0,\"size\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.items[0].score").isNumber());

        mockMvc.perform(get("/api/admin/heat-exchangers").with(user(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanExportFilteredCatalogToExcel() throws Exception {
        UserPrincipal principal = principal(Role.USER, "excel-user");
        byte[] content = mockMvc.perform(post("/api/heat-exchangers/export")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"families\":[\"AIR_COOLED\"]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertThat(workbook.getSheet("Теплообменники")).isNotNull();
            assertThat(workbook.getSheet("Теплообменники").getLastRowNum()).isEqualTo(6);
            assertThat(workbook.getSheet("Теплообменники").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Производитель");
        }
    }

    @Test
    void staleAdminUpdateReturnsStructuredConflict() throws Exception {
        var exchanger = repository.findAllWithDetails().get(0);
        StatusUpdate stale = new StatusUpdate(CatalogStatus.ARCHIVED, exchanger.getVersion() + 1);
        User admin = userRepository.findByUsername("admin").orElseThrow();

        mockMvc.perform(patch("/api/admin/heat-exchangers/{id}/status", exchanger.getId())
                        .with(user(new UserPrincipal(admin)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stale)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATALOG_CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/admin/heat-exchangers/" + exchanger.getId() + "/status"));
    }

    private UserPrincipal principal(Role role, String username) {
        User value = User.builder()
                .username(username)
                .passwordHash("not-used")
                .email(username + "@example.test")
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        return new UserPrincipal(userRepository.saveAndFlush(value));
    }
}
