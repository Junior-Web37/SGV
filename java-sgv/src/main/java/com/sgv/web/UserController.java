package com.sgv.web;

import com.sgv.entity.User;
import com.sgv.repository.UserRepository;
import com.sgv.repository.RoleRepository;
import com.sgv.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserRepository userRepository, UserService userService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<UserDTO> list() {
        return userRepository.findAll().stream().map(UserDTO::from).collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasAuthority('USERS:CREATE')")
    public ResponseEntity<UserDTO> create(@Valid @RequestBody CreateUserRequest req) {
        User u = userService.registerWithRoles(req.username, req.password, req.fullName, req.roleIds, req.permissions);
        return ResponseEntity.ok(UserDTO.from(u));
    }

    public static class CreateUserRequest {
        @NotBlank(message = "Username é obrigatório")
        @Size(min = 3, max = 50, message = "Username deve ter entre 3 e 50 caracteres")
        public String username;

        @NotBlank(message = "Password é obrigatória")
        @Size(min = 8, message = "Password deve ter pelo menos 8 caracteres")
        public String password;

        public String fullName;
        public java.util.Set<Long> roleIds;
        public java.util.Set<String> permissions;
    }
}
