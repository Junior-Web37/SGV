package com.sgv.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("isAuthenticated()")
public class PermissionsController {

    @GetMapping("/catalog")
    @PreAuthorize("hasRole('SUPERADMIN') or hasAuthority('ROLES:VIEW')")
    public List<String> catalog() {
        return PermissionsCatalog.ALL;
    }
}
