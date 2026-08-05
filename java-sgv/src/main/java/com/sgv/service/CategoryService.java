package com.sgv.service;

import com.sgv.entity.Category;
import com.sgv.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
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
        if (category == null) throw new IllegalArgumentException("Category is null");
        String trimmedName = category.getName() == null ? "" : category.getName().trim();
        if (trimmedName.isBlank()) throw new IllegalArgumentException("Nome é obrigatório.");
        if (existsByName(trimmedName, category.getId())) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }
        category.setName(trimmedName);
        return categoryRepository.save(category);
    }
}
