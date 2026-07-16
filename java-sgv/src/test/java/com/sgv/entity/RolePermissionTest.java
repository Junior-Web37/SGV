package com.sgv.entity;

import com.sgv.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RolePermissionTest {

    @Test
    void shouldCheckPermissionsByPageAndAction() {
        Role role = new Role();
        role.setName("CAIXA");
        role.setPermissions(Set.of("VENDAS:VIEW", "VENDAS:CREATE"));

        assertTrue(role.hasPermission("VENDAS", "VIEW"));
        assertTrue(role.hasPermission("VENDAS", "CREATE"));
        assertFalse(role.hasPermission("VENDAS", "DELETE"));
        assertFalse(role.hasPermission("PRODUTOS", "VIEW"));
    }

    @Test
    void shouldAllowWildcardPermissionsForUser() {
        Role role = new Role();
        role.setName("ADMIN");
        role.setPermissions(Set.of("*:VIEW", "VENDAS:*"));

        User user = new User();
        user.setRoles(Set.of(role));

        assertTrue(user.hasPermission("PRODUTOS", "VIEW"));
        assertTrue(user.hasPermission("VENDAS", "CREATE"));
        assertFalse(user.hasPermission("CAIXA", "DELETE"));
    }
}
