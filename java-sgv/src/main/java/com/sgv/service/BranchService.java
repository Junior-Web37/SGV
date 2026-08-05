package com.sgv.service;

import com.sgv.entity.Branch;
import com.sgv.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public List<Branch> findAll() {
        return branchRepository.findAll();
    }

    public Optional<Branch> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return branchRepository.findByNameIgnoreCase(name.trim());
    }

    public boolean existsByName(String name, Long excludeId) {
        return findByName(name)
                .filter(branch -> excludeId == null || !excludeId.equals(branch.getId()))
                .isPresent();
    }

    @Transactional
    public Branch saveBranch(Branch branch) {
        if (branch == null) throw new IllegalArgumentException("Branch is null");
        String trimmedName = branch.getName() == null ? "" : branch.getName().trim();
        if (trimmedName.isBlank()) throw new IllegalArgumentException("Nome é obrigatório.");
        if (existsByName(trimmedName, branch.getId())) {
            throw new IllegalArgumentException("Nome de filial já existe.");
        }
        branch.setName(trimmedName);
        return branchRepository.save(branch);
    }
}
