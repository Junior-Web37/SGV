-- V15: Fix filter_presets.data column type to LONGTEXT for @Lob mapping
ALTER TABLE filter_presets MODIFY COLUMN data LONGTEXT NULL;
