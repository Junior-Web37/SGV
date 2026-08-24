package com.sgv.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserRolePermissionTest {

    private Role role(String name, String... permissions) {
        Role role = new Role();
        role.setName(name);
        role.setPermissions(Set.of(permissions));
        return role;
    }

    private User user(Role... roles) {
        User user = new User();
        user.setRoles(Set.of(roles));
        return user;
    }

    @Test
    void hasPermission_specificPermission_returnsTrue() {
        assertTrue(user(role("VENDEDOR", "CAIXA:VIEW", "PRODUTOS:VIEW"))
                .hasPermission("CAIXA", "VIEW"));
    }

    @Test
    void hasPermission_missingPermission_returnsFalse() {
        assertFalse(user(role("VENDEDOR", "CAIXA:VIEW"))
                .hasPermission("PRODUTOS", "CREATE"));
    }

    @Test
    void hasPermission_wildcardPermission_returnsTrue() {
        assertTrue(user(role("ADMIN", "*:*")).hasPermission("QUALQUER", "QUALQUER"));
    }

    @Test
    void hasPermission_pageWildcard_returnsTrue() {
        assertTrue(user(role("GERENTE", "CAIXA:*")).hasPermission("CAIXA", "DELETE"));
    }

    @Test
    void hasPermission_queryIsCaseInsensitive() {
        assertTrue(user(role("VENDEDOR", "CAIXA:VIEW")).hasPermission("caixa", "view"));
    }

    @Test
    void hasPermission_storedPermissionIsCaseInsensitive() {
        assertTrue(user(role("VENDEDOR", "caixa:view")).hasPermission("CAIXA", "VIEW"));
        assertTrue(user(role("VENDEDOR", "caixa:*")).hasPermission("CAIXA", "DELETE"));
        assertTrue(user(role("VENDEDOR", "*:delete")).hasPermission("CAIXA", "DELETE"));
        assertTrue(user(role("VENDEDOR", "*:*")).hasPermission("CAIXA", "DELETE"));
    }

    @Test
    void hasPermission_storedPermissionWithoutColon_isIgnored() {
        assertFalse(user(role("VENDEDOR", "caixaview")).hasPermission("CAIXA", "VIEW"));
    }

    @Test
    void hasPermission_nullPermissionEntry_doesNotThrow() {
        Role r = role("VENDEDOR", "CAIXA:VIEW");
        java.util.HashSet<String> perms = new java.util.HashSet<>(r.getPermissions());
        perms.add(null);
        r.setPermissions(perms);
        User u = new User();
        u.setRoles(Set.of(r));
        assertTrue(u.hasPermission("CAIXA", "VIEW"));
    }

    @Test
    void hasPermission_noRoles_returnsFalse() {
        User user = new User();
        assertFalse(user.hasPermission("CAIXA", "VIEW"));
    }

    @Test
    void isSuperAdmin_wildcardRole_returnsTrue() {
        assertTrue(user(role("ADMIN", "*:*")).isSuperAdmin());
    }

    @Test
    void isSuperAdmin_normalRole_returnsFalse() {
        assertFalse(user(role("VENDEDOR", "CAIXA:VIEW")).isSuperAdmin());
    }

    @Test
    void isProtectedAdmin_adminUsername_returnsTrueEvenWithoutRoles() {
        User admin = new User();
        admin.setUsername("admin");
        assertTrue(admin.isProtectedAdmin());
    }

    @Test
    void isProtectedAdmin_superAdmin_returnsTrue() {
        User admin = new User();
        admin.setUsername("manager");
        admin.setRoles(Set.of(role("ADMIN", "*:*")));
        assertTrue(admin.isProtectedAdmin());
    }

    @Test
    void isProtectedAdmin_normalUser_returnsFalse() {
        User u = new User();
        u.setUsername("joao");
        u.setRoles(Set.of(role("VENDEDOR", "CAIXA:VIEW")));
        assertFalse(u.isProtectedAdmin());
    }

    @Test
    void rolePermission_wildcardAction_returnsTrue() {
        assertTrue(role("R", "PRODUTOS:*").hasPermission("produtos", "CREATE"));
    }

    @Test
    void rolePermission_actionWildcard_returnsTrue() {
        assertTrue(role("R", "*:CREATE").hasPermission("RELATORIOS", "CREATE"));
    }
}
