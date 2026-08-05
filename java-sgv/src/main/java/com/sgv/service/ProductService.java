package com.sgv.service;

import com.sgv.entity.Product;
import com.sgv.entity.ProductBarcode;
import com.sgv.repository.ProductBarcodeRepository;
import com.sgv.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductBarcodeRepository productBarcodeRepository;
    private final SystemLogService systemLogService;

    public ProductService(ProductRepository productRepository,
                          ProductBarcodeRepository productBarcodeRepository,
                          SystemLogService systemLogService) {
        this.productRepository = productRepository;
        this.productBarcodeRepository = productBarcodeRepository;
        this.systemLogService = systemLogService;
    }

    public Optional<Product> findByCode(String code) { return productRepository.findByCode(code); }
    public Optional<Product> findByNameIgnoreCase(String name) { return productRepository.findByNameIgnoreCase(name); }
    public List<Product> findAllActive() { return productRepository.findAllActive(); }
    public Optional<Product> findByBarcodeOrCode(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return productBarcodeRepository.findByBarcode(text)
                .or(() -> productBarcodeRepository.findByBarcodeIgnoreCase(text))
                .map(ProductBarcode::getProduct)
                .or(() -> productRepository.findByCode(text));
    }
    public long findMaxId() { try { Long v = productRepository.findMaxId(); return v != null ? v : 0L; } catch (Exception e) { return 0L; } }

    @Transactional
    public Product saveProduct(Product p, List<ProductBarcode> pendingBarcodes) {
        if (p == null) throw new IllegalArgumentException("Product is null");
        Product saved = productRepository.save(p);
        if (pendingBarcodes != null) {
            for (ProductBarcode pb : pendingBarcodes) {
                if (pb.getId() == null) {
                    pb.setProduct(saved);
                    productBarcodeRepository.save(pb);
                }
            }
        }
        return saved;
    }

    @Transactional
    public void deleteById(Long id) { productRepository.deleteById(id); }

    public boolean barcodeExists(String barcode) { return productBarcodeRepository.findByBarcodeIgnoreCase(barcode).isPresent(); }
    public List<ProductBarcode> findBarcodesByProduct(Product p) { return productBarcodeRepository.findByProductOrderByBarcode(p); }

    @Transactional
    public void saveBarcode(ProductBarcode pb) { productBarcodeRepository.save(pb); }

    @Transactional
    public void deleteBarcodeById(Long id) { productBarcodeRepository.deleteById(id); }
}
