package com.sgv.service;

import com.sgv.entity.Supplier;
import com.sgv.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> findAll() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> findById(Long id) {
        return supplierRepository.findById(id);
    }

    public Optional<Supplier> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return supplierRepository.findByNameIgnoreCase(name.trim());
    }

    public boolean existsByName(String name, Long excludeId) {
        return findByName(name)
                .filter(supplier -> excludeId == null || !excludeId.equals(supplier.getId()))
                .isPresent();
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier == null) throw new IllegalArgumentException("Supplier is null");
        String trimmedName = supplier.getName() == null ? "" : supplier.getName().trim();
        if (trimmedName.isBlank()) throw new IllegalArgumentException("Nome é obrigatório.");
        if (existsByName(trimmedName, supplier.getId())) {
            throw new IllegalArgumentException("Nome de fornecedor já existe.");
        }
        supplier.setName(trimmedName);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteById(Long id) {
        supplierRepository.deleteById(id);
    }
}
