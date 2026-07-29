package com.sgv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;
    private String email;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private boolean forceChangePassword = false;
    private boolean canViewStats = false;
    private double commissionPercent = 0.0;
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    @JsonIgnore
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public boolean isForceChangePassword() { return forceChangePassword; }
    public void setForceChangePassword(boolean forceChangePassword) { this.forceChangePassword = forceChangePassword; }
    public boolean isCanViewStats() { return canViewStats; }
    public void setCanViewStats(boolean canViewStats) { this.canViewStats = canViewStats; }
    public double getCommissionPercent() { return commissionPercent; }
    public void setCommissionPercent(double commissionPercent) { this.commissionPercent = commissionPercent; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean hasPermission(String page, String action) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().filter(java.util.Objects::nonNull).anyMatch(role -> role.hasPermission(page, action));
    }

    public boolean isSuperAdmin() {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().filter(java.util.Objects::nonNull)
                .anyMatch(role -> "SUPERADMIN".equalsIgnoreCase(role.getName()) || role.hasPermission("*", "*"));
    }

    public boolean isProtectedAdmin() {
        if (username != null && username.equalsIgnoreCase("admin")) {
            return true;
        }
        return isSuperAdmin();
    }

    @Override
    public String toString() {
        if (fullName != null && !fullName.isBlank()) return fullName;
        return username != null ? username : "Utilizador";
    }
}
