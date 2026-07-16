package com.sgv.dto;

import com.sgv.entity.User;
import java.util.stream.Collectors;

public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private boolean active;
    private java.util.Set<String> roles;

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.active = user.isActive();
        response.roles = user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());
        return response;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public java.util.Set<String> getRoles() { return roles; }
}
