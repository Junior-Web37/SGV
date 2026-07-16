package com.sgv.web;

import com.sgv.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GhostSuperadminTest {

    @Autowired
    UserService userService;

    @Test
    void loadGhostSuperadminHasRole() {
        var ud = userService.loadUserByUsername("superadmin");
        assertNotNull(ud);
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")));
    }
}
