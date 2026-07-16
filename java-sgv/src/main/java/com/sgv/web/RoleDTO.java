package com.sgv.web;

import com.sgv.entity.Role;
import java.util.HashSet;
import java.util.Set;

public class RoleDTO {
    public Long id;
    public String name;
    public String description;
    public Set<String> permissions = new HashSet<>();

    public static RoleDTO from(Role r) {
        RoleDTO dto = new RoleDTO();
        dto.id = r.getId();
        dto.name = r.getName();
        dto.description = r.getDescription();
        if (r.getPermissions() != null) dto.permissions = new HashSet<>(r.getPermissions());
        return dto;
    }

    public Role toEntity() {
        Role r = new Role();
        r.setId(this.id);
        r.setName(this.name);
        r.setDescription(this.description);
        r.setPermissions(this.permissions != null ? new HashSet<>(this.permissions) : new HashSet<>());
        return r;
    }
}
