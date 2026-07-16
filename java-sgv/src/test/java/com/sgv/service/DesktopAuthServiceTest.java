package com.sgv.service;

import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesktopAuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private DesktopAuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new DesktopAuthService(userRepository, passwordEncoder, "superadmin", "SuperSecret123!");
    }

    @Test
    void shouldAuthenticateAdminFromDatabase() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setActive(true);
        admin.setPasswordHash("encoded");
        Role role = new Role();
        role.setName("ADMIN");
        admin.setRoles(Set.of(role));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin", "encoded")).thenReturn(true);

        User result = authService.authenticate("admin", "admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertFalse(result.isSuperAdmin());
        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    void shouldAuthenticateGhostSuperadminWithoutDatabaseLookup() {
        User result = authService.authenticate("superadmin", "SuperSecret123!");

        assertNotNull(result);
        assertEquals("superadmin", result.getUsername());
        assertTrue(result.isSuperAdmin());
        assertTrue(result.hasPermission("VENDAS", "CREATE"));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void shouldRejectInactiveAdminAccount() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setActive(false);
        admin.setPasswordHash("encoded");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User result = authService.authenticate("admin", "admin");

        assertNull(result);
        verify(userRepository, times(1)).findByUsername("admin");
    }
}
