package com.sgv.desktop;

import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.AuditLogRepository;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.RoleRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.AuditLogService;
import com.sgv.service.DesktopAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SistemaModuleControllerTest {

    private SistemaModuleController controller;
    private UserRepository userRepository;
    private BranchRepository branchRepository;
    private RoleRepository roleRepository;
    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;
    private DesktopAuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() throws Exception {
        userRepository = mock(UserRepository.class);
        branchRepository = mock(BranchRepository.class);
        roleRepository = mock(RoleRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = mock(AuditLogService.class);
        authService = mock(DesktopAuthService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        controller = new SistemaModuleController();
        setPrivateField(controller, "userRepository", userRepository);
        setPrivateField(controller, "branchRepository", branchRepository);
        setPrivateField(controller, "roleRepository", roleRepository);
        setPrivateField(controller, "auditLogRepository", auditLogRepository);
        setPrivateField(controller, "auditLogService", auditLogService);
        setPrivateField(controller, "authService", authService);
        setPrivateField(controller, "passwordEncoder", passwordEncoder);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void canManageSystem_returnsTrueForSuperAdminGhost() {
        User user = new User();
        Role role = new Role();
        role.setName("SUPERADMIN");
        role.setPermissions(Set.of("*:*"));
        user.setRoles(Set.of(role));
        controller.setCurrentUser(user);

        assertTrue(controller.canManageSystem());
    }

    @Test
    void canManageSystem_returnsTrueForAdminWithSistemaView() {
        User user = new User();
        Role role = new Role();
        role.setName("ADMIN");
        role.setPermissions(Set.of("SISTEMA:VIEW"));
        user.setRoles(Set.of(role));
        controller.setCurrentUser(user);

        assertTrue(controller.canManageSystem());
    }

    @Test
    void canManageSystem_returnsFalseForUnauthorizedUser() {
        User user = new User();
        Role role = new Role();
        role.setName("CAIXA");
        role.setPermissions(Set.of("VENDAS:VIEW"));
        user.setRoles(Set.of(role));
        controller.setCurrentUser(user);

        assertFalse(controller.canManageSystem());
    }
}
