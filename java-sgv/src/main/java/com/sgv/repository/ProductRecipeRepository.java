package com.sgv.repository;

import com.sgv.entity.ProductRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {
    List<ProductRecipe> findByParentProductId(Long parentProductId);
    void deleteByParentProductId(Long parentProductId);
}
