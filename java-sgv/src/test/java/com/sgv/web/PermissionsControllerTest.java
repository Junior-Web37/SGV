package com.sgv.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import com.sgv.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.main.web-application-type=servlet")
@Import(TestConfig.class)
@AutoConfigureMockMvc
public class PermissionsControllerTest {

    MockMvc mvc;

    @Autowired
    WebApplicationContext wac;

    @BeforeEach
    void setup() {
        if (this.mvc == null) {
            this.mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        }
    }

    @Test
    void catalogAvailable() throws Exception {
        mvc.perform(get("/api/permissions/catalog")).andExpect(status().isOk());
    }
}
