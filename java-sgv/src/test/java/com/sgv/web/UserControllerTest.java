package com.sgv.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.main.web-application-type=servlet")
@Import(TestConfig.class)
@AutoConfigureMockMvc
public class UserControllerTest {

    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    RoleRepository roleRepository;
    @Autowired
    com.sgv.repository.UserRepository userRepository;

    @Autowired
    WebApplicationContext wac;

    @BeforeEach
    void setup() {
        if (this.mvc == null) {
            this.mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        }
    }

    @Test
    void createUserWithRole() throws Exception {
        // ensure role exists
        var role = roleRepository.findByName("CLIENTE").orElseGet(() -> {
            var r = new com.sgv.entity.Role(); r.setName("CLIENTE"); return roleRepository.save(r);
        });

        // clean up any leftover test user
        userRepository.findByUsername("testuser").ifPresent(u -> userRepository.delete(u));

        UserController.CreateUserRequest req = new UserController.CreateUserRequest();
        req.username = "testuser";
        req.password = "pass";
        req.fullName = "Test User";
        req.roleIds = Collections.singleton(role.getId());

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req))).andExpect(status().isOk());
    }
}
