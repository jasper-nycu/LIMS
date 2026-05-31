package com.tsmc.lims.backend.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void accessProtectedMachineApi_WithoutJwtToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/machines"))
               .andExpect(status().isForbidden());
    }

    @Test
    void accessPublicErrorApi_WithoutJwtToken_ShouldBypassSecurity() throws Exception {
        mockMvc.perform(get("/error"))
               .andExpect(status().isInternalServerError());
    }
}