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
        "security.auth.lock-minutes=1",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
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

        String access = rt.get("accessToken").asText();
        mvc.perform(get("/api/audit-logs").header("Authorization","Bearer "+access)).andExpect(status().isOk());
    }
}
