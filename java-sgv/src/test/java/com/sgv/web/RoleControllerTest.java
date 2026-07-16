package com.sgv.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgv.entity.Role;
import com.sgv.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import com.sgv.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.main.web-application-type=servlet")
@Import(TestConfig.class)
@AutoConfigureMockMvc
public class RoleControllerTest {

    MockMvc mvc;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ObjectMapper mapper;

    @BeforeEach
    void setup() {
        if (this.mvc == null) {
            this.mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        }
    }

    @Test
    void createAndGetRole() throws Exception {
        // use unique role name to avoid collisions with other tests/data
        String roleName = "TEST_ROLE_" + System.currentTimeMillis();

        RoleDTO dto = new RoleDTO();
        dto.name = roleName;
        dto.description = "role desc";
        dto.permissions = Collections.singleton("VENDAS:ACCESS");

        mvc.perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(roleName));
    }
}
