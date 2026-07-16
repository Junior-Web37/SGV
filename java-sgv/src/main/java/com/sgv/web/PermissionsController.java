package com.sgv.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionsController {

    @GetMapping("/catalog")
    public List<String> catalog() {
        return PermissionsCatalog.ALL;
    }
}
