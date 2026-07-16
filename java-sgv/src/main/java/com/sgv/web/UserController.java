package com.sgv.web;

import com.sgv.entity.User;
import com.sgv.repository.UserRepository;
import com.sgv.repository.RoleRepository;
import com.sgv.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
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
    public ResponseEntity<UserDTO> create(@RequestBody CreateUserRequest req) {
        User u = userService.registerWithRoles(req.username, req.password, req.fullName, req.roleIds, req.permissions);
        return ResponseEntity.ok(UserDTO.from(u));
    }

    public static class CreateUserRequest {
        public String username;
        public String password;
        public String fullName;
        public java.util.Set<Long> roleIds;
        public java.util.Set<String> permissions;
    }
}
