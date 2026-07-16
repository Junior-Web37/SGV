package com.sgv.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions != null ? permissions : new HashSet<>(); }

    public boolean hasPermission(String page, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        String normalizedPage = page != null ? page.toUpperCase() : "";
        String normalizedAction = action != null ? action.toUpperCase() : "";
        return permissions.contains("*:*")
                || permissions.contains(normalizedPage + ":" + normalizedAction)
                || permissions.contains(normalizedPage + ":*")
                || permissions.contains("*:" + normalizedAction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }

    @Override
    public String toString() {
        return name != null ? name : "Papel";
    }
}
