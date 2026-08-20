package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_recipes")
public class ProductRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_product_id", nullable = false)
    private Product parentProduct;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_product_id", nullable = false)
    private Product ingredientProduct;

    @Column(name = "quantity_required", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantityRequired = BigDecimal.ONE;

    @Column(length = 50)
    private String unit = "UN";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getParentProduct() { return parentProduct; }
    public void setParentProduct(Product parentProduct) { this.parentProduct = parentProduct; }

    public Product getIngredientProduct() { return ingredientProduct; }
    public void setIngredientProduct(Product ingredientProduct) { this.ingredientProduct = ingredientProduct; }

    public BigDecimal getQuantityRequired() { return quantityRequired != null ? quantityRequired : BigDecimal.ZERO; }
    public void setQuantityRequired(BigDecimal quantityRequired) { this.quantityRequired = quantityRequired; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
