-- V24: Criar tabela product_recipes para ficha técnica / BOM de produtos compostos e padaria

CREATE TABLE IF NOT EXISTS product_recipes (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_product_id      BIGINT NOT NULL,
    ingredient_product_id  BIGINT NOT NULL,
    quantity_required      DECIMAL(19, 4) NOT NULL DEFAULT 1.0000 COMMENT 'Quantidade de ingrediente necessária por unidade do produto final',
    unit                   VARCHAR(50) DEFAULT 'UN',
    CONSTRAINT fk_recipe_parent FOREIGN KEY (parent_product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_ingredient FOREIGN KEY (ingredient_product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_product_recipes_parent ON product_recipes (parent_product_id);
CREATE INDEX IF NOT EXISTS idx_product_recipes_ingredient ON product_recipes (ingredient_product_id);

-- End V24
