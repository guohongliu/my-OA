package com.example.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class RBACAndCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String login(String u, String p) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\""+u+"\",\"password\":\""+p+"\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode j = om.readTree(r.getResponse().getContentAsString());
        return j.get("accessToken").asText();
    }

    @Test
    void rbac_and_crud() throws Exception {
        String admin = login("admin","admin123");

        MvcResult orgRes = mvc.perform(post("/api/orgs").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"name\":\"DeptA\",\"code\":\"DA\"}"))
                .andExpect(status().isOk()).andReturn();
        long orgId = om.readTree(orgRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult empRes = mvc.perform(post("/api/employees").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"name\":\"Alice\",\"email\":\"a@x.com\",\"orgId\":"+orgId+"}"))
                .andExpect(status().isOk()).andReturn();
        long empId = om.readTree(empRes.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/employees/"+empId).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"id\":"+empId+",\"name\":\"Alice2\",\"email\":\"a2@x.com\",\"orgId\":"+orgId+"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/orgs/tree").header("Authorization","Bearer "+admin)).andExpect(status().isOk());

        String user = login("user","user123");
        mvc.perform(delete("/api/employees/"+empId).header("Authorization","Bearer "+user)).andExpect(status().isForbidden());

        mvc.perform(delete("/api/employees/"+empId).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
    }
}
