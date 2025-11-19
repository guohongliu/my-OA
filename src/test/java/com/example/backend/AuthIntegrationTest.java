package com.example.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.auth.max-failed=2",
        "security.auth.lock-minutes=1"
})
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserAccountRepository userRepo;

    @Test
    void authFlow_lock_rotate_and_audit() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"user\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"user\",\"password\":\"bad\"}"))
                .andExpect(status().isLocked());

        MvcResult ok = mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode tokens = om.readTree(ok.getResponse().getContentAsString());
        String refresh = tokens.get("refreshToken").asText();

        MvcResult ref = mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{\"refreshToken\":\""+refresh+"\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode rt = om.readTree(ref.getResponse().getContentAsString());
        String newRefresh = rt.get("refreshToken").asText();

        mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{\"refreshToken\":\""+refresh+"\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/audit-logs")).andExpect(status().isOk());
    }
}