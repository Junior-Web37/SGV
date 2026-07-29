package com.sgv.service;

import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.RoleRepository;
import com.sgv.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String rawPassword, String fullName) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        // assign default role CLIENTE
        Role r = roleRepository.findByName("CLIENTE").orElseGet(() -> {
            Role nr = new Role();
            nr.setName("CLIENTE");
            nr.setDescription("Cliente padrão");
            return roleRepository.save(nr);
        });
        u.getRoles().add(r);
        return userRepository.save(u);
    }

    public User registerWithRoles(String username, String rawPassword, String fullName, Set<Long> roleIds, Set<String> permissions) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        if (roleIds != null && !roleIds.isEmpty()) {
            roleIds.forEach(id -> roleRepository.findById(id).ifPresent(u.getRoles()::add));
        } else {
            Role r = roleRepository.findByName("CLIENTE").orElseGet(() -> {
                Role nr = new Role();
                nr.setName("CLIENTE");
                nr.setDescription("Cliente padrão");
                return roleRepository.save(nr);
            });
            u.getRoles().add(r);
        }

        // If explicit per-user permissions are provided, create a dedicated role for them
        if (permissions != null && !permissions.isEmpty()) {
            String roleName = "USER_PERMS_" + (username != null ? username.toUpperCase() : "") + "_" + System.currentTimeMillis();
            Role permRole = new Role();
            permRole.setName(roleName);
            permRole.setDescription("Permissões específicas do utilizador " + username);
            permRole.setPermissions(permissions.stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toSet()));
            permRole = roleRepository.save(permRole);
            u.getRoles().add(permRole);
        }
        return userRepository.save(u);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<SimpleGrantedAuthority> auths = u.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
        auths.add(new SimpleGrantedAuthority("ROLE_ACTIVE"));
        return new org.springframework.security.core.userdetails.User(u.getUsername(), u.getPasswordHash(), auths);
    }
}
