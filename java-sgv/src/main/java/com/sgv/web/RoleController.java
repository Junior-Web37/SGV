package com.sgv.web;

import com.sgv.entity.Role;
import com.sgv.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<RoleDTO> list() {
        return roleRepository.findAll().stream().map(RoleDTO::from).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> get(@PathVariable Long id) {
        Optional<Role> r = roleRepository.findById(id);
        return r.map(role -> ResponseEntity.ok(RoleDTO.from(role))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RoleDTO> create(@RequestBody RoleDTO dto) {
        Role r = dto.toEntity();
        r.setId(null);
        Role saved = roleRepository.save(r);
        return ResponseEntity.ok(RoleDTO.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        Optional<Role> ex = roleRepository.findById(id);
        if (ex.isEmpty()) return ResponseEntity.notFound().build();
        Role r = ex.get();
        r.setName(dto.name);
        r.setDescription(dto.description);
        r.setPermissions(dto.permissions);
        Role saved = roleRepository.save(r);
        return ResponseEntity.ok(RoleDTO.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) return ResponseEntity.notFound().build();
        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
