-- V14: Fix product flow integrity — unique constraints, warehouse tracking, stock alerts

-- BUG#2: Unique constraint on stock_branch to prevent duplicate records
ALTER TABLE stock_branch ADD UNIQUE INDEX uq_stock_branch_product_branch (product_id, branch_id);

-- BUG#11+3: Add warehouse_id to stock_movements for per-warehouse queries
ALTER TABLE stock_movements ADD COLUMN warehouse_id BIGINT NULL;
ALTER TABLE stock_movements ADD INDEX idx_movements_warehouse (warehouse_id);
