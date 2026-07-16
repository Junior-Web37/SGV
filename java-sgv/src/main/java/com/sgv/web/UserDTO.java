package com.sgv.web;

import com.sgv.entity.Role;
import com.sgv.entity.User;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDTO {
    public Long id;
    public String username;
    public String fullName;
    public String email;
    public Set<Long> roleIds = new HashSet<>();

    public static UserDTO from(User u) {
        UserDTO d = new UserDTO();
        d.id = u.getId();
        d.username = u.getUsername();
        d.fullName = u.getFullName();
        d.email = u.getEmail();
        if (u.getRoles() != null) d.roleIds = u.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        return d;
    }
}
