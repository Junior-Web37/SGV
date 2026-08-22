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
        if (username == null || rawPassword == null) {
            return null;
        }
        var userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            log.warn("[AUTH] Tentativa de login falhada: utilizador '{}' não registado", username);
            return null;
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("[AUTH] Tentativa de login recusada: utilizador '{}' encontra-se inativo", username);
            return null;
        }
        String hash = user.getPasswordHash();
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        if (!matches) {
            log.warn("[AUTH] Falha de autenticação: credenciais inválidas para o utilizador '{}'", username);
            return null;
        }
        log.info("[AUTH] Autenticação bem-sucedida para o utilizador '{}' (id={})", username, user.getId());
        return user;
    }
}
