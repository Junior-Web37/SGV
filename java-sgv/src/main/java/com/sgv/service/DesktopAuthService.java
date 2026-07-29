package com.sgv.service;

import com.sgv.entity.User;
import com.sgv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DesktopAuthService {

    private static final Logger log = LoggerFactory.getLogger(DesktopAuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DesktopAuthService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String username, String rawPassword) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("[AUTH] User '{}' nao encontrado na BD", username);
            return null;
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("[AUTH] User '{}' esta inativo (active=false)", username);
            return null;
        }
        String hash = user.getPasswordHash();
        log.debug("[AUTH] User '{}' encontrado (id={}, active=true, hash_prefix={})", username, user.getId(), hash != null ? hash.substring(0, Math.min(10, hash.length())) : "null");
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        if (!matches) {
            log.warn("[AUTH] Password nao corresponde para user '{}' (hash_prefix={})", username, hash != null ? hash.substring(0, Math.min(20, hash.length())) : "null");
            return null;
        }
        log.info("[AUTH] Autenticacao bem-sucedida para user '{}'", username);
        return user;
    }
}

