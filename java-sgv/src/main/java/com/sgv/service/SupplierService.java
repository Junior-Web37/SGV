package com.sgv.service;

import com.sgv.entity.Supplier;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.SupplierPaymentRepository;
import com.sgv.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           PurchaseRepository purchaseRepository,
                           SupplierPaymentRepository supplierPaymentRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
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
        if (supplier == null) throw new IllegalArgumentException("Fornecedor é obrigatório.");
        String trimmedName = supplier.getName() == null ? "" : supplier.getName().trim();
        if (trimmedName.isBlank()) throw new IllegalArgumentException("Nome do fornecedor é obrigatório.");
        if (existsByName(trimmedName, supplier.getId())) {
            throw new IllegalArgumentException("Nome de fornecedor já existe.");
        }
        supplier.setName(trimmedName);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) return;
        int purchaseCount = purchaseRepository.findBySupplierIdOrderByCreatedAtDesc(id).size();
        int paymentCount = supplierPaymentRepository.findBySupplierId(id).size();
        if (purchaseCount > 0 || paymentCount > 0) {
            throw new IllegalStateException("Não é possível eliminar fornecedor com histórico de transações (" +
                    purchaseCount + " compras, " + paymentCount + " pagamentos). Recomenda-se desativá-lo.");
        }
        supplierRepository.deleteById(id);
    }
}
