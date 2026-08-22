package com.sgv.service;

import com.sgv.entity.Category;
import com.sgv.repository.CategoryRepository;
import com.sgv.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Optional<Category> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return categoryRepository.findByNameIgnoreCase(name.trim());
    }

    public boolean existsByName(String name, Long excludeId) {
        return findByName(name)
                .filter(existing -> excludeId == null || !excludeId.equals(existing.getId()))
                .isPresent();
    }

    @Transactional
    public Category saveCategory(Category category) {
        if (category == null) throw new IllegalArgumentException("Categoria é obrigatória.");
        String trimmedName = category.getName() == null ? "" : category.getName().trim();
        if (trimmedName.isBlank()) throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        if (existsByName(trimmedName, category.getId())) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }
        category.setName(trimmedName);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) return;
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Não é possível eliminar a categoria porque existem " + productCount + " artigo(s) associado(s).");
        }
        categoryRepository.deleteById(id);
    }
}
