-- ==============================================================================
-- V27: POVOAMENTO COMPLETO E EXTENSO — COMÉRCIO, PADARIA, SERVIÇOS & LOGÍSTICA (MZ)
-- ==============================================================================

-- 1. FILIAIS / LOJAS EM MOÇAMBIQUE
INSERT IGNORE INTO branches (id, name, nuit, address, contact, is_head, software_cert_number, license_number) VALUES
(1, 'Sede Maputo (Baixa)', '400123456', 'Av. 24 de Julho, nº 1420, Maputo', '+258 84 123 4567', 1, 'CERT-AT-2026/0042', 'SGV-MZ-MAP-001'),
(2, 'Filial Matola (Fomento)', '400123456', 'Av. Samora Machel, nº 850, Matola', '+258 84 234 5678', 0, 'CERT-AT-2026/0042', 'SGV-MZ-MAT-002'),
(3, 'Filial Beira (Ponta Gea)', '400123456', 'Av. das FPLM, nº 310, Beira', '+258 84 345 6789', 0, 'CERT-AT-2026/0042', 'SGV-MZ-BEI-003'),
(4, 'Filial Nampula (Centro)', '400123456', 'Av. Eduardo Mondlane, nº 120, Nampula', '+258 84 456 7890', 0, 'CERT-AT-2026/0042', 'SGV-MZ-NAM-004');

-- 2. ARMAZÉNS CENTRAIS & DEPÓSITOS DE DISTRIBUIÇÃO
INSERT IGNORE INTO warehouses (id, code, name, nuit, address, contact, notes, is_active, created_at) VALUES
(1, 'ARM-MACHAVA', 'Armazém Central da Machava', '400123456', 'Estrada Nacional nº 4, Zona Industrial da Machava', '+258 84 555 1000', 'Centro de distribuição geral e estoque regulador', 1, NOW()),
(2, 'DEP-PORTO', 'Depósito Portuário de Maputo', '400123456', 'Recinto Portuário, Cais nº 4, Maputo', '+258 84 555 2000', 'Recepção de contentores e importação direta', 1, NOW()),
(3, 'ARM-BEIRA', 'Armazém Regional da Beira', '400123456', 'Bairro da Munhava, Beira', '+258 84 555 3000', 'Abastecimento da região centro', 1, NOW());

-- 3. UNIDADES DE MEDIDA COMERCIAIS
INSERT IGNORE INTO metric_units (id, abbreviation, description) VALUES
(1, 'UN', 'Unidade Individual'),
(2, 'KG', 'Quilograma'),
(3, 'G', 'Grama'),
(4, 'L', 'Litro'),
(5, 'ML', 'Mililitro'),
(6, 'CX', 'Caixa Comercial'),
(7, 'SAC', 'Saco Industrial (25kg/50kg)'),
(8, 'PCT', 'Pacote'),
(9, 'FD', 'Fardo / Shrink'),
(10, 'HR', 'Hora de Serviço'),
(11, 'DIA', 'Diária de Equipamento/Viatura');

-- 4. CATEGORIAS / FAMÍLIAS DE ARTIGOS
INSERT IGNORE INTO categories (id, name) VALUES
(1, 'Mercearia & Grãos Essenciais'),
(2, 'Bebidas & Refrigerantes'),
(3, 'Padaria & Pastelaria'),
(4, 'Lacticínios & Frios'),
(5, 'Higiene & Limpeza'),
(6, 'Matérias-Primas & Insumos de Produção'),
(7, 'Serviços Comerciais, Transporte & Frete');

-- 5. FORNECEDORES EM MOÇAMBIQUE
INSERT IGNORE INTO suppliers (id, name, nuit, contact, address, active) VALUES
(1, 'Companhia Industrial da Matola (CIM), SA', '400111222', '+258 21 720 000', 'Rua da CIM, Matola', 1),
(2, 'Cervejas de Moçambique (CDM), SA', '400222333', '+258 21 300 100', 'Estrada Nacional nº 1, Marracuene', 1),
(3, 'Coca-Cola Sabco Moçambique, Lda', '400333444', '+258 21 750 200', 'Av. de Moçambique, Km 8, Machava', 1),
(4, 'Açucareira de Moçambique, SA', '400444555', '+258 23 380 400', 'Mafambisse, Dondo, Sofala', 1),
(5, 'Distribuidora Alimentar do Zimpeto, Lda', '400555666', '+258 84 900 8000', 'Mercado Grossista do Zimpeto, Maputo', 1),
(6, 'Moçambique Lacticínios & Frios, Lda', '400666777', '+258 84 800 7000', 'Bairro do Jardim, Maputo', 1);

-- 6. CATÁLOGO COMPLETO DE ARTIGOS & SERVIÇOS
INSERT IGNORE INTO products (
    id, code, name, category_id, supplier_id, unit_id,
    price_cost, price_sale, price_sale_bulk, bulk_quantity, profit_margin,
    tax_rate, ice_rate, default_tax_rate, default_ice_rate,
    stock_min, stock_max, is_active, is_service, description, location
) VALUES
-- 6.1 Bens de Primeira Necessidade (Isentos IVA - Artigo 9º CIVA / M02)
(1, 'GRAO-001', 'Farinha de Trigo Nacional 50kg', 1, 1, 7, 2100.0000, 2450.0000, 2350.0000, 10.0000, 16.6667, 0.0000, 0.0000, 0.0000, 0.0000, 20.0000, 500.0000, 1, 0, 'Farinha de trigo tipo 1 para panificação e comércio', 'Armazém A / Palete 01'),
(2, 'GRAO-002', 'Arroz Agulha Especial 25kg', 1, 5, 7, 1250.0000, 1450.0000, 1380.0000, 10.0000, 16.0000, 0.0000, 0.0000, 0.0000, 0.0000, 30.0000, 400.0000, 1, 0, 'Arroz longo grão fino importado', 'Armazém A / Palete 02'),
(3, 'GRAO-003', 'Açúcar Branco Nacional 1kg', 1, 4, 8, 48.0000, 60.0000, 55.0000, 20.0000, 25.0000, 0.0000, 0.0000, 0.0000, 0.0000, 50.0000, 1000.0000, 1, 0, 'Açúcar branco de cana de Moçambique', 'Corredor 1 / Prateleira A'),
(4, 'GRAO-004', 'Óleo Alimentar Vegetal 5L', 1, 5, 4, 450.0000, 540.0000, 510.0000, 4.0000, 20.0000, 0.0000, 0.0000, 0.0000, 0.0000, 20.0000, 200.0000, 1, 0, 'Óleo puro refinado sem colesterol', 'Corredor 1 / Prateleira B'),
(5, 'LACT-001', 'Leite Pasteurizado Integral 1L', 4, 6, 4, 75.0000, 95.0000, 90.0000, 12.0000, 26.6667, 0.0000, 0.0000, 0.0000, 0.0000, 40.0000, 300.0000, 1, 0, 'Leite gordo enriquecido com vitaminas', 'Câmara Frigorífica 1'),

-- 6.2 Artigos de Padaria & Fabrico Próprio (Isentos Artigo 9º CIVA)
(6, 'PAD-001', 'Pão Francês Tradicional 50g', 3, NULL, 1, 4.5000, 10.0000, 8.5000, 50.0000, 122.2222, 0.0000, 0.0000, 0.0000, 0.0000, 100.0000, 2000.0000, 1, 0, 'Pão de trigo crocante forno a lenha', 'Balcão de Padaria'),
(7, 'PAD-002', 'Pão de Forma Integral 500g', 3, NULL, 1, 45.0000, 75.0000, 68.0000, 10.0000, 66.6667, 0.0000, 0.0000, 0.0000, 0.0000, 20.0000, 200.0000, 1, 0, 'Pão de forma fatiado rico em fibras', 'Balcão de Padaria'),
(8, 'PAD-003', 'Bolo Caseiro de Chocolate (Fatia)', 3, NULL, 1, 35.0000, 80.0000, 70.0000, 10.0000, 128.5714, 16.0000, 0.0000, 16.0000, 0.0000, 10.0000, 100.0000, 1, 0, 'Fatia de bolo com cobertura de brigadeiro', 'Vitrina Confeitaria'),

-- 6.3 Matérias-Primas & Insumos de Padaria (BOM)
(9, 'MAT-001', 'Farinha de Trigo Especial Padaria 50kg', 6, 1, 7, 2050.0000, 2400.0000, 2300.0000, 5.0000, 17.0732, 0.0000, 0.0000, 0.0000, 0.0000, 15.0000, 300.0000, 1, 0, 'Saco de farinha de alta força W300', 'Depósito Matérias-Primas'),
(10, 'MAT-002', 'Fermento Biológico Seco 500g', 6, 5, 8, 110.0000, 150.0000, 135.0000, 10.0000, 36.3636, 16.0000, 0.0000, 16.0000, 0.0000, 10.0000, 100.0000, 1, 0, 'Fermento de ação rápida para massas', 'Depósito Matérias-Primas'),
(11, 'MAT-003', 'Sal Refinado Industrial 25kg', 6, 5, 7, 300.0000, 420.0000, 380.0000, 5.0000, 40.0000, 16.0000, 0.0000, 16.0000, 0.0000, 5.0000, 50.0000, 1, 0, 'Sal iodado seco', 'Depósito Matérias-Primas'),
(12, 'MAT-004', 'Margarina Industrial 15kg', 6, 6, 6, 1200.0000, 1650.0000, 1500.0000, 2.0000, 37.5000, 16.0000, 0.0000, 16.0000, 0.0000, 5.0000, 40.0000, 1, 0, 'Gordura vegetal para massas folhadas e pães', 'Câmara Frigorífica 2'),

-- 6.4 Bebidas & Refrigerantes (Tributados IVA 16%)
(13, 'BEB-001', 'Água Mineral Vumba 500ml', 2, 5, 1, 15.0000, 25.0000, 20.0000, 24.0000, 66.6667, 16.0000, 0.0000, 16.0000, 0.0000, 50.0000, 1000.0000, 1, 0, 'Água mineral natural sem gás', 'Prateleira Bebidas'),
(14, 'BEB-002', 'Água Mineral Namaacha 1.5L', 2, 5, 1, 28.0000, 45.0000, 40.0000, 12.0000, 60.7143, 16.0000, 0.0000, 16.0000, 0.0000, 30.0000, 500.0000, 1, 0, 'Água de nascente pura', 'Prateleira Bebidas'),
(15, 'BEB-003', 'Refrigerante Coca-Cola 330ml Lata', 2, 3, 1, 35.0000, 50.0000, 45.0000, 24.0000, 42.8571, 16.0000, 0.0000, 16.0000, 0.0000, 50.0000, 800.0000, 1, 0, 'Lata alumínio refrigerante cola', 'Geleira 1'),
(16, 'BEB-004', 'Refrigerante Fanta Laranja 2L', 2, 3, 1, 75.0000, 110.0000, 100.0000, 6.0000, 46.6667, 16.0000, 0.0000, 16.0000, 0.0000, 20.0000, 300.0000, 1, 0, 'Garrafa PET 2 Litros', 'Geleira 1'),
(17, 'BEB-005', 'Cerveja 2M 330ml Garrafa', 2, 2, 1, 45.0000, 65.0000, 60.0000, 24.0000, 44.4444, 16.0000, 0.0000, 16.0000, 0.0000, 50.0000, 1200.0000, 1, 0, 'Cerveja nacional clara', 'Geleira 2'),
(18, 'BEB-006', 'Cerveja Laurentina Preta 330ml', 2, 2, 1, 55.0000, 80.0000, 75.0000, 24.0000, 45.4545, 16.0000, 0.0000, 16.0000, 0.0000, 30.0000, 600.0000, 1, 0, 'Cerveja preta especial', 'Geleira 2'),

-- 6.5 Higiene & Limpeza (Tributados IVA 16%)
(19, 'HIG-001', 'Sabão em Barra Azul 1kg', 5, 5, 1, 60.0000, 85.0000, 78.0000, 10.0000, 41.6667, 16.0000, 0.0000, 16.0000, 0.0000, 25.0000, 400.0000, 1, 0, 'Sabão multiuso para lavagem', 'Corredor 3 / Prateleira A'),
(20, 'HIG-002', 'Detergente em Pó OMO 500g', 5, 5, 8, 70.0000, 98.0000, 90.0000, 12.0000, 40.0000, 16.0000, 0.0000, 16.0000, 0.0000, 30.0000, 300.0000, 1, 0, 'Detergente perfumado ação profunda', 'Corredor 3 / Prateleira B'),

-- 6.6 Serviços Comerciais (is_service = 1, sem movimentação de stock físico)
(21, 'SRV-001', 'Serviço de Frete & Entrega Maputo/Matola', 7, NULL, 1, 150.0000, 450.0000, 400.0000, 1.0000, 200.0000, 16.0000, 0.0000, 16.0000, 0.0000, 0.0000, 0.0000, 1, 1, 'Entrega de mercadorias ao domicílio ou loja do cliente', 'Serviço'),
(22, 'SRV-002', 'Aluguer de Carrinha de Distribuição (Diária)', 7, NULL, 11, 1200.0000, 3500.0000, 3000.0000, 5.0000, 191.6667, 16.0000, 0.0000, 16.0000, 0.0000, 0.0000, 0.0000, 1, 1, 'Aluguer de carrinha 3.5T com motorista para transporte comercial', 'Serviço'),
(23, 'SRV-003', 'Consultoria & Assessoria de Gestão Comercial', 7, NULL, 10, 500.0000, 1500.0000, 1200.0000, 10.0000, 200.0000, 16.0000, 0.0000, 16.0000, 0.0000, 0.0000, 0.0000, 1, 1, 'Hora de consultoria especializada para inventários e auditoria', 'Serviço');

-- 7. CÓDIGOS DE BARRAS MULTIPLOS
INSERT IGNORE INTO product_barcodes (id, product_id, barcode, quantity) VALUES
(1, 13, '6001234567890', 1.0000),  -- Agua Vumba Unidade
(2, 13, '16001234567897', 24.0000), -- Agua Vumba Pack 24
(3, 15, '5449000000996', 1.0000),  -- Coca-Cola Lata
(4, 15, '15449000000993', 24.0000), -- Coca-Cola Pack 24
(5, 17, '6001108000018', 1.0000),  -- Cerveja 2M
(6, 17, '16001108000015', 24.0000), -- Caixa 2M 24 Garrafas
(7, 3,  '6002223334445', 1.0000);  -- Acucar 1kg

-- 8. FICHAS TÉCNICAS / RECEITAS (BOM) DA PADARIA
INSERT IGNORE INTO product_recipes (id, parent_product_id, ingredient_product_id, quantity_required, unit) VALUES
-- Pão Francês 50g (ID 6): 35g Farinha (ID 9), 1g Fermento (ID 10), 0.5g Sal (ID 11)
(1, 6, 9, 0.0350, 'KG'),
(2, 6, 10, 0.0010, 'KG'),
(3, 6, 11, 0.0005, 'KG'),
-- Pão de Forma 500g (ID 7): 350g Farinha (ID 9), 8g Fermento (ID 10), 15g Margarina (ID 12), 10g Açúcar (ID 3)
(4, 7, 9, 0.3500, 'KG'),
(5, 7, 10, 0.0080, 'KG'),
(6, 7, 12, 0.0150, 'KG'),
(7, 7, 3, 0.0100, 'KG');

-- 9. CLIENTES EM MOÇAMBIQUE (RETALHO, GROSSO & CONTAS CORRENTES)
INSERT IGNORE INTO customers (id, code, name, nuit, type, contact, address, credit_limit, balance, default_discount, fidelity_points, created_at) VALUES
(1, 'CLI-0001', 'Consumidor Final (Diversos)', '999999999', 'RETALHO', '+258 84 000 0000', 'Balcão de Venda, Maputo', 0.0000, 0.0000, 0.0000, 0, NOW()),
(2, 'CLI-0002', 'Supermercados Recheio de Maputo, Lda', '400777888', 'GROSSO', '+258 21 400 500', 'Av. das Indústrias, nº 204, Matola', 150000.0000, 35000.0000, 5.0000, 120, NOW()),
(3, 'CLI-0003', 'Padaria & Pastelaria Flor de Maputo, Lda', '400888999', 'GROSSO', '+258 84 311 2233', 'Av. Mao Tse Tung, nº 450, Maputo', 80000.0000, 18500.0000, 3.0000, 45, NOW()),
(4, 'CLI-0004', 'Hotel & Restaurante Polana Mar, SA', '400999000', 'GROSSO', '+258 21 490 000', 'Av. Marginal, nº 100, Maputo', 250000.0000, 62000.0000, 7.5000, 300, NOW()),
(5, 'CLI-0005', 'Mercearia Central do Fomento', '400123789', 'RETALHO', '+258 84 555 4444', 'Bairro do Fomento, Matola', 40000.0000, 0.0000, 2.0000, 15, NOW());

-- 10. UTILIZADORES COM PERFIS REAIS DE MOÇAMBIQUE
-- Password padrão para todos: 'admin' (Hash BCrypt válido)
INSERT IGNORE INTO users (id, username, password_hash, full_name, email, active, branch_id, can_view_stats, commission_percent, force_change_password) VALUES
(1, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Administrador Geral', 'admin@sgv.co.mz', 1, 1, 1, 0.0000, 0),
(2, 'gerente.maputo', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Manuel Cossa (Gerente)', 'manuel.cossa@sgv.co.mz', 1, 1, 1, 1.5000, 0),
(3, 'caixa1.maputo', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Ana Paula Mondlane (Caixa)', 'ana.mondlane@sgv.co.mz', 1, 1, 0, 0.5000, 0),
(4, 'fiel.machava', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'António Macamo (Fiel Armazém)', 'antonio.macamo@sgv.co.mz', 1, 1, 0, 0.0000, 0),
(5, 'contabilidade', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Dr. Alberto Sitoe (Contabilista)', 'alberto.sitoe@sgv.co.mz', 1, 1, 1, 0.0000, 0);

-- Associar papéis
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
(1, 1), -- Admin -> ADMIN
(2, 2), -- Gerente -> GESTOR
(3, 3), -- Caixa -> CAIXA
(4, 2), -- Fiel -> GESTOR
(5, 1); -- Contabilidade -> ADMIN

-- 11. POVOAMENTO INICIAL DE STOCK EM LOJAS (Filial 1 - Maputo e Filial 2 - Matola)
INSERT IGNORE INTO stock_branch (product_id, branch_id, stock_current, stock_min, stock_max, version) VALUES
(1, 1, 80.0000, 10.0000, 200.0000, 0),   -- Farinha 50kg Maputo
(2, 1, 120.0000, 15.0000, 300.0000, 0),  -- Arroz 25kg Maputo
(3, 1, 450.0000, 30.0000, 800.0000, 0),  -- Açúcar 1kg Maputo
(4, 1, 60.0000, 10.0000, 150.0000, 0),   -- Óleo 5L Maputo
(5, 1, 90.0000, 15.0000, 200.0000, 0),   -- Leite 1L Maputo
(6, 1, 350.0000, 50.0000, 1500.0000, 0), -- Pão Francês Maputo
(7, 1, 40.0000, 10.0000, 100.0000, 0),   -- Pão Forma Maputo
(13, 1, 300.0000, 30.0000, 600.0000, 0), -- Água Vumba Maputo
(15, 1, 240.0000, 24.0000, 500.0000, 0), -- Coca-Cola Lata Maputo
(17, 1, 480.0000, 48.0000, 1000.0000, 0),-- Cerveja 2M Maputo
(9, 1, 40.0000, 5.0000, 100.0000, 0),    -- Matéria-prima Farinha Padaria Maputo
(10, 1, 15.0000, 2.0000, 50.0000, 0),    -- Fermento Padaria Maputo
-- Filial Matola
(1, 2, 45.0000, 10.0000, 150.0000, 0),
(2, 2, 70.0000, 10.0000, 200.0000, 0),
(3, 2, 280.0000, 20.0000, 500.0000, 0),
(6, 2, 200.0000, 30.0000, 800.0000, 0),
(13, 2, 150.0000, 20.0000, 400.0000, 0),
(17, 2, 240.0000, 24.0000, 600.0000, 0);

-- 12. POVOAMENTO DE STOCK CENTRAL NOS ARMAZÉNS (Armazém 1 - Machava)
INSERT IGNORE INTO stock_warehouse (warehouse_id, product_id, stock_current, stock_min, stock_max, version) VALUES
(1, 1, 800.0000, 100.0000, 2000.0000, 0), -- 800 sacos farinha 50kg
(1, 2, 650.0000, 80.0000, 1500.0000, 0),  -- 650 sacos arroz 25kg
(1, 3, 2500.0000, 200.0000, 5000.0000, 0),-- 2500 kg açúcar
(1, 4, 400.0000, 50.0000, 1000.0000, 0),  -- 400 garrafões óleo 5L
(1, 9, 300.0000, 50.0000, 800.0000, 0),   -- 300 sacos farinha padaria
(1, 10, 80.0000, 10.0000, 200.0000, 0),   -- 80 kg fermento seco
(1, 13, 1500.0000, 200.0000, 4000.0000, 0),-- 1500 águas Vumba
(1, 15, 1200.0000, 150.0000, 3000.0000, 0),-- 1200 latas Coca-Cola
(1, 17, 2400.0000, 200.0000, 5000.0000, 0);-- 2400 garrafas 2M

-- 13. DESPESAS OPERACIONAIS PADRÃO
INSERT IGNORE INTO expenses (id, description, category, amount, due_date, paid_at, state, notes, branch_id, user_id, created_at) VALUES
(1, 'Renda das Instalações da Sede (Mês Atual)', 'RENDA', 45000.0000, CURDATE(), NOW(), 'PAID', 'Doc: REC-RENDA-2026/08', 1, 1, NOW()),
(2, 'Factura de Electricidade EDM (Maputo)', 'UTILITIES', 12850.0000, CURDATE(), NOW(), 'PAID', 'Doc: EDM-CONTA-998877', 1, 1, NOW()),
(3, 'Factura de Água FIPAG (Maputo)', 'UTILITIES', 3420.0000, CURDATE(), NOW(), 'PAID', 'Doc: FIPAG-REC-4433', 1, 1, NOW()),
(4, 'Abastecimento de Combustível Carrinha 3.5T (TotalEnergies)', 'TRANSPORTE', 8500.0000, CURDATE(), NOW(), 'PAID', 'Doc: TAL-TOTAL-77665', 1, 2, NOW()),
(5, 'Manutenção Preventiva Forno de Padaria', 'MANUTENÇÃO', 6500.0000, DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL, 'PENDING', 'Doc: ORC-TECNICO-104', 1, 2, NOW());

-- 14. CONFIGURAÇÃO GERAL DA EMPRESA
INSERT INTO app_config (
    id, company_name, company_nuit, company_address, company_phone, company_email,
    default_series, default_tax_rate, default_currency, exchange_rate,
    initial_document_number, thermal_printer_width, max_discount_percent,
    consumer_final_nuit, auto_backup_enabled, offline_mode_enabled, demo_mode, setup_completed, setup_completed_at
) VALUES (
    1, 'Empresa Comercial SGV Moçambique, SA', '400123456',
    'Av. 24 de Julho, nº 1420, Maputo, Moçambique',
    '+258 84 123 4567 / +258 21 300 000', 'contacto@sgv.co.mz',
    'A', 16.0000, 'MZN', 74.0000, 1, 80, 10.0000, '999999999', 1, 1, 0, 1, NOW()
) ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    company_nuit = VALUES(company_nuit),
    company_address = VALUES(company_address),
    company_phone = VALUES(company_phone),
    default_currency = 'MZN',
    default_tax_rate = 16.0000;

-- End V27
