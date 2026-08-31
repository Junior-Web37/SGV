package com.billywater.config;

import com.billywater.servico.ServicoSeguranca;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Migração da base de dados (DDL) + seed de arranque. Idempotente. */
public final class MigracaoBanco {

    private MigracaoBanco() {}

    public static void migrar() {
        Connection c = ConfigBanco.get();
        try (Statement st = c.createStatement()) {
            criarDominio(st);
            criarFigurao(st);
            criarComercial(st);
            criarTerceiros(st);
            criarLogs(st);
            garantirVersao(st);
            seed(st);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha na migração BILLY WATER: " + e.getMessage(), e);
        }
    }

    private static void criarDominio(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS utilizador (id BIGINT AUTO_INCREMENT PRIMARY KEY, nome_completo VARCHAR(150), username VARCHAR(60) UNIQUE, senha_hash VARCHAR(255), email VARCHAR(120), perfil VARCHAR(30), ativo BOOLEAN DEFAULT TRUE, forcar_mudanca_senha BOOLEAN DEFAULT FALSE, tentativas_falhadas INT DEFAULT 0, bloqueado BOOLEAN DEFAULT FALSE, criado_em TIMESTAMP)");
        st.execute("CREATE TABLE IF NOT EXISTS parametro (id BIGINT PRIMARY KEY, nome_empresa VARCHAR(150), nuit VARCHAR(50), alvara VARCHAR(80), endereco VARCHAR(200), cidade VARCHAR(80), telefone VARCHAR(40), email VARCHAR(120), rodape_documental VARCHAR(255), iva_geral DECIMAL(10,2), percentagem_iva_agua DECIMAL(10,2), taxa_saneamento DECIMAL(10,2), taxa_religacao DECIMAL(10,2), dia_limite_pagamento INT, serie_ft VARCHAR(10), serie_vd VARCHAR(10), pasta_backups VARCHAR(120), impressora_termica VARCHAR(120), impressao_termica_ativa BOOLEAN DEFAULT FALSE)");
        st.execute("CREATE TABLE IF NOT EXISTS zona (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(20), nome VARCHAR(120), municipio VARCHAR(80), bairro VARCHAR(80), rota VARCHAR(30), descricao VARCHAR(200), ativo BOOLEAN DEFAULT TRUE, ativa BOOLEAN DEFAULT TRUE, total_clientes INT DEFAULT 0, media_historica DECIMAL(12,2))");
        st.execute("CREATE TABLE IF NOT EXISTS armazem (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(20), nome VARCHAR(120), localizacao VARCHAR(160), tipo VARCHAR(20), ativo BOOLEAN DEFAULT TRUE)");
    }

    private static void criarFigurao(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS cliente (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30), nome_completo VARCHAR(180), nuit VARCHAR(50), nif VARCHAR(50), email VARCHAR(120), telefone VARCHAR(40), endereco VARCHAR(200), cidade VARCHAR(80), bairro VARCHAR(80), quarteirao VARCHAR(40), casa VARCHAR(40), tipo VARCHAR(20), ativo BOOLEAN DEFAULT TRUE, limite_credito DECIMAL(14,2), saldo DECIMAL(14,2), contrato_id BIGINT, rota_id BIGINT, contador_id BIGINT, categoria_tarifaria VARCHAR(30), criado_em TIMESTAMP)");
        st.execute("CREATE TABLE IF NOT EXISTS contador (id BIGINT AUTO_INCREMENT PRIMARY KEY, numero VARCHAR(40), numero_serie VARCHAR(60), marca VARCHAR(60), modelo VARCHAR(60), calibre VARCHAR(30), leitura_instalacao DECIMAL(14,3), leitura_inicial DECIMAL(14,3), data_instalacao DATE, data_ultima_leitura DATE, ultimo_consumo DECIMAL(14,3), ultima_leitura DECIMAL(14,3), estado VARCHAR(30), cliente_id BIGINT, contrato_id BIGINT, rota_id BIGINT, zona_id BIGINT, multiplicador INT DEFAULT 1, localizacao VARCHAR(160))");
        st.execute("CREATE TABLE IF NOT EXISTS contrato (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30), numero_conta VARCHAR(40), cliente_id BIGINT, contador_id BIGINT, tipo VARCHAR(30), categoria VARCHAR(30), categoria_tarifaria VARCHAR(30), zona_id BIGINT, tarifa_id BIGINT, rota_id BIGINT, data_inicio DATE, data_fim DATE, estado VARCHAR(30), limite_credito DECIMAL(14,2), numero_cliente VARCHAR(40), ciclo_facturacao VARCHAR(20), dia_leitura INT DEFAULT 1)");
    }

    private static void criarComercial(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS tarifa (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30), nome VARCHAR(120), categoria VARCHAR(30), vigencia DATE, minimo_facturavel DECIMAL(14,3), taxa_disponibilidade DECIMAL(12,2), taxa_saneamento DECIMAL(12,2), percentagem_iva_base DECIMAL(10,2), isento_iva_minimo BOOLEAN DEFAULT TRUE, vigencia_inicio DATE, vigencia_fim DATE, ativo BOOLEAN DEFAULT TRUE)");
        st.execute("CREATE TABLE IF NOT EXISTS escalao_tarifa (id BIGINT AUTO_INCREMENT PRIMARY KEY, tarifa_id BIGINT, limite_inferior DECIMAL(14,3), limite_superior DECIMAL(14,3), preco_por_m3 DECIMAL(14,4), taxa_iva DECIMAL(10,2))");
        st.execute("CREATE TABLE IF NOT EXISTS leitura (id BIGINT AUTO_INCREMENT PRIMARY KEY, contador_id BIGINT, contrato_id BIGINT, cliente_id BIGINT, rota_id BIGINT, periodo VARCHAR(7), leitura_anterior DECIMAL(14,3), leitura_actual DECIMAL(14,3), consumo DECIMAL(14,3), tipo VARCHAR(20), estado VARCHAR(30), estimada BOOLEAN DEFAULT FALSE, anomalia VARCHAR(255), data_leitura DATE, leitor VARCHAR(80), facturada BOOLEAN DEFAULT FALSE, criado_em TIMESTAMP)");
        st.execute("CREATE TABLE IF NOT EXISTS factura (id BIGINT AUTO_INCREMENT PRIMARY KEY, tipo_documento VARCHAR(5), serie VARCHAR(10), numero BIGINT, cliente_id BIGINT, contador_id BIGINT, contrato_id BIGINT, periodo VARCHAR(7), consumo DECIMAL(14,3), valor_agua DECIMAL(14,2), taxa_disponibilidade DECIMAL(14,2), saneamento DECIMAL(14,2), subtotal DECIMAL(14,2), iva DECIMAL(14,2), total DECIMAL(14,2), limite_pagamento DECIMAL(14,2), estado VARCHAR(30), metodo_pagamento VARCHAR(30), criado_em TIMESTAMP, pagamento_id BIGINT, anulada BOOLEAN DEFAULT FALSE, estimada BOOLEAN DEFAULT FALSE)");
        st.execute("CREATE TABLE IF NOT EXISTS linha_factura (id BIGINT AUTO_INCREMENT PRIMARY KEY, factura_id BIGINT, tipo_linha VARCHAR(20), descricao VARCHAR(255), quantidade DECIMAL(14,3), preco_unitario DECIMAL(14,4), subtotal DECIMAL(14,2), base_iva DECIMAL(14,2), iva DECIMAL(14,2), taxa_iva DECIMAL(10,2))");
    }

    private static void criarTerceiros(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS fornecedor (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30), nome VARCHAR(160), nuit VARCHAR(50), telefone VARCHAR(40), email VARCHAR(120), endereco VARCHAR(200), cidade VARCHAR(80), ativo BOOLEAN DEFAULT TRUE, saldo DECIMAL(14,2))");
        st.execute("CREATE TABLE IF NOT EXISTS compra (id BIGINT AUTO_INCREMENT PRIMARY KEY, serie VARCHAR(10), numero BIGINT, fornecedor_id BIGINT, armazem_id BIGINT, valor_total DECIMAL(14,2), referencia VARCHAR(80), data_ DATE, estado VARCHAR(20), operador VARCHAR(60))");
        st.execute("CREATE TABLE IF NOT EXISTS linha_compra (id BIGINT AUTO_INCREMENT PRIMARY KEY, compra_id BIGINT, produto_id BIGINT, quantidade DECIMAL(14,3), custo_unitario DECIMAL(14,4), subtotal DECIMAL(14,2))");
        st.execute("CREATE TABLE IF NOT EXISTS producao (id BIGINT AUTO_INCREMENT PRIMARY KEY, data_ DATE, periodo VARCHAR(7), volume_produzido DECIMAL(16,3), volume_facturado DECIMAL(16,3), perdas DECIMAL(16,3), nrw_percent DECIMAL(10,2), fonte VARCHAR(60))");
        st.execute("CREATE TABLE IF NOT EXISTS permissao (id BIGINT AUTO_INCREMENT PRIMARY KEY, perfil VARCHAR(30), pagina VARCHAR(30), acao VARCHAR(30), permitir BOOLEAN DEFAULT TRUE, UNIQUE(perfil, pagina, acao))");
    }

    private static void criarLogs(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS sessao_caixa (id BIGINT AUTO_INCREMENT PRIMARY KEY, abertura TIMESTAMP, fecho TIMESTAMP, operador VARCHAR(60), fundo_maneio DECIMAL(14,2), valor_esperado DECIMAL(14,2), valor_contado DECIMAL(14,2), diferenca DECIMAL(14,2), estado VARCHAR(20), operador_fecho VARCHAR(60), criada_em TIMESTAMP)");
        st.execute("CREATE TABLE IF NOT EXISTS movimento_caixa (id BIGINT AUTO_INCREMENT PRIMARY KEY, sessao_id BIGINT, tipo VARCHAR(5), valor DECIMAL(14,2), origem VARCHAR(30), referencia VARCHAR(80), criado_em TIMESTAMP, operador VARCHAR(60))");
        st.execute("CREATE TABLE IF NOT EXISTS pagamento (id BIGINT AUTO_INCREMENT PRIMARY KEY, factura_id BIGINT, cliente_id BIGINT, metodo VARCHAR(30), valor DECIMAL(14,2), valor_recebido DECIMAL(14,2), troco DECIMAL(14,2), rec_numero BIGINT, operador VARCHAR(60), sessao_caixa_id BIGINT, criado_em TIMESTAMP, estornado BOOLEAN DEFAULT FALSE)");
        st.execute("ALTER TABLE pagamento ADD COLUMN IF NOT EXISTS estornado BOOLEAN DEFAULT FALSE");
        st.execute("CREATE TABLE IF NOT EXISTS produto (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30), codigo_barras VARCHAR(80), nome VARCHAR(160), categoria VARCHAR(60), unidade VARCHAR(20), preco_compra DECIMAL(14,2), preco_venda DECIMAL(14,2), iva DECIMAL(10,2), stock_actual DECIMAL(14,3), stock_minimo DECIMAL(14,3), servico BOOLEAN DEFAULT FALSE, ativo BOOLEAN DEFAULT TRUE)");
        st.execute("CREATE TABLE IF NOT EXISTS stock_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, produto_id BIGINT, armazem_id BIGINT, quantidade DECIMAL(14,3), stock_minimo DECIMAL(14,3), stock_maximo DECIMAL(14,3), estado VARCHAR(20))");
        st.execute("CREATE TABLE IF NOT EXISTS movimento_stock (id BIGINT AUTO_INCREMENT PRIMARY KEY, produto_id BIGINT, armazem_id BIGINT, tipo VARCHAR(40), quantidade DECIMAL(14,3), stock_antes DECIMAL(14,3), stock_depois DECIMAL(14,3), referencia VARCHAR(80), criado_em TIMESTAMP, operador VARCHAR(60))");
        st.execute("CREATE TABLE IF NOT EXISTS venda (id BIGINT AUTO_INCREMENT PRIMARY KEY, serie VARCHAR(10), numero BIGINT, cliente_id BIGINT, factura_agua_id BIGINT, subtotal DECIMAL(14,2), desconto DECIMAL(14,2), iva DECIMAL(14,2), total DECIMAL(14,2), forma_pagamento VARCHAR(30), devolvida BOOLEAN DEFAULT FALSE, anulada BOOLEAN DEFAULT FALSE, criado_em TIMESTAMP, operador VARCHAR(60))");
        st.execute("CREATE TABLE IF NOT EXISTS linha_venda (id BIGINT AUTO_INCREMENT PRIMARY KEY, venda_id BIGINT, produto_id BIGINT, quantidade DECIMAL(14,3), preco DECIMAL(14,4), subtotal DECIMAL(14,2), devolvido DECIMAL(14,3))");
        st.execute("CREATE TABLE IF NOT EXISTS transferencia_armazem (id BIGINT AUTO_INCREMENT PRIMARY KEY, serie VARCHAR(10), numero BIGINT, origem_id BIGINT, destino_id BIGINT, estado VARCHAR(20), referencia VARCHAR(80), criado_em TIMESTAMP, operador VARCHAR(60))");
        st.execute("CREATE TABLE IF NOT EXISTS trabalho (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30), cliente_id BIGINT, descricao VARCHAR(255), tecnico VARCHAR(80), estado VARCHAR(30), mao_obra DECIMAL(14,2), materiais DECIMAL(14,2), subtotal DECIMAL(14,2), iva DECIMAL(14,2), total DECIMAL(14,2), criado_em TIMESTAMP, factura_servico_id BIGINT)");
        st.execute("CREATE TABLE IF NOT EXISTS ordem_servico (id BIGINT AUTO_INCREMENT PRIMARY KEY, tipo VARCHAR(30), cliente_id BIGINT, contador_id BIGINT, estado VARCHAR(30), criada_em DATE, tecnico VARCHAR(80), taxa_cobrada DECIMAL(14,2), motivo VARCHAR(255))");
        st.execute("CREATE TABLE IF NOT EXISTS prestacao (id BIGINT AUTO_INCREMENT PRIMARY KEY, plano_id BIGINT, numero INT, valor DECIMAL(14,2), vencimento DATE, pago DECIMAL(14,2), estado VARCHAR(20), data_pagamento DATE)");
        st.execute("CREATE TABLE IF NOT EXISTS auditoria (id BIGINT AUTO_INCREMENT PRIMARY KEY, data_ TIMESTAMP, utilizador VARCHAR(60), categoria VARCHAR(30), acao VARCHAR(60), entidade VARCHAR(80), detalhe VARCHAR(500))");
        st.execute("CREATE TABLE IF NOT EXISTS versao_bd (versao INT)");
    }

    private static void garantirVersao(Statement st) throws SQLException {
        st.execute("MERGE INTO versao_bd KEY(versao) VALUES (1)");
    }

    private static void seed(Statement st) throws SQLException {
        try (var rs = st.executeQuery("SELECT COUNT(*) FROM utilizador")) {
            if (rs.next() && rs.getLong(1) == 0) {
                st.executeUpdate("INSERT INTO utilizador (nome_completo, username, senha_hash, email, perfil, ativo, forcar_mudanca_senha) " +
                        "VALUES ('Administrador', 'admin', '" + ServicoSeguranca.hash("admin123") + "', 'admin@billywater.mz', 'ADMIN', TRUE, TRUE)");
            }
        }
        try (var rs = st.executeQuery("SELECT COUNT(*) FROM parametro")) {
            if (rs.next() && rs.getLong(1) == 0) {
                st.executeUpdate("INSERT INTO parametro (id, nome_empresa, nuit, alvara, endereco, cidade, telefone, email, rodape_documental, iva_geral, percentagem_iva_agua, taxa_saneamento, taxa_religacao, dia_limite_pagamento, serie_ft, serie_vd, pasta_backups) " +
                        "VALUES (1, 'BILLY WATER', '100000000', 'A/2024', 'Av. 25 de Setembro, Maputo', 'Maputo', '+258 84 000 0000', 'geral@billywater.mz', 'Documento emitido por BILLY WATER — Concessionária de Água', 16, 75, 0, 750, 10, 'FT', 'VD', 'backups')");
            }
        }
        try (var rs = st.executeQuery("SELECT COUNT(*) FROM zona")) {
            if (rs.next() && rs.getLong(1) == 0) {
                st.executeUpdate("INSERT INTO zona (id, codigo, nome, municipio, bairro, rota) VALUES (1, 'Z-01', 'Central', 'Maputo', 'Baixa', 'R1'), (2, 'Z-02', 'Costa do Sol', 'Maputo', 'Sommerschield', 'R2'), (3, 'Z-03', 'Altos', 'Maputo', 'Alto Maé', 'R3')");
            }
        }
        try (var rs = st.executeQuery("SELECT COUNT(*) FROM armazem")) {
            if (rs.next() && rs.getLong(1) == 0) {
                st.executeUpdate("INSERT INTO armazem (id, codigo, nome, localizacao, tipo) VALUES (1, 'AR-01', 'Armazém Central', 'Polana, Maputo', 'ARMAZEM'), (2, 'LO-01', 'Loja do Balcão', 'Matola', 'LOJA')");
            }
        }
        try (var rs = st.executeQuery("SELECT COUNT(*) FROM tarifa")) {
            if (rs.next() && rs.getLong(1) == 0) demoComercial(st);
        }
    }

    private static void demoComercial(Statement st) throws SQLException {
        st.executeUpdate("INSERT INTO tarifa (id, codigo, nome, categoria, minimo_facturavel, taxa_disponibilidade, taxa_saneamento, percentagem_iva_base, isento_iva_minimo, ativo) VALUES (1, 'T-DOM', 'Doméstico (AURA)', 'DOMESTICO', 5, 250, 0, 75, TRUE, TRUE)");
        st.executeUpdate("INSERT INTO escalao_tarifa (tarifa_id, limite_inferior, limite_superior, preco_por_m3) VALUES (1, 0, 5, 28.60), (1, 5, 10, 42.90), (1, 10, 25, 57.20), (1, 25, 50, 85.80), (1, 50, 0, 137.28)");
        st.executeUpdate("INSERT INTO cliente (id, codigo, nome_completo, nuit, tipo, telefone, endereco, cidade, bairro, ativo, limite_credito, saldo, criado_em) VALUES (1, 'C-0001', 'Maria da Silva', '100123456', 'SINGULAR', '+258 84 000 0001', 'Av. da Baixa, 12', 'Maputo', 'Baixa', TRUE, 5000, 0, NOW())");
        st.executeUpdate("INSERT INTO contador (id, numero, numero_serie, marca, modelo, estado, leitura_instalacao, leitura_inicial, multiplicador, rota_id, zona_id) VALUES (1, 'CT-0001', 'K-2024-0001', 'Kamstrup', 'MultiJet', 'ACTIVO', 0, 0, 1, 1, 1)");
        st.executeUpdate("INSERT INTO contrato (id, codigo, numero_conta, cliente_id, contador_id, categoria, categoria_tarifaria, zona_id, rota_id, tarifa_id, estado, data_inicio, ciclo_facturacao, dia_leitura) VALUES (1, 'L-0001', 'N-000001', 1, 1, 'DOMESTICO', 'DOMESTICO', 1, 1, 1, 'ACTIVO', CURRENT_DATE, 'MENSAL', 10)");
        st.executeUpdate("INSERT INTO leitura (id, contador_id, contrato_id, cliente_id, rota_id, periodo, leitura_anterior, leitura_actual, consumo, tipo, estado, estimada, facturada, criado_em) VALUES (1, 1, 1, 1, 1, '" + java.time.YearMonth.now() + "', 0, 7, 7, 'MEDIDA', 'VALIDADA', FALSE, TRUE, NOW())");
        st.executeUpdate("INSERT INTO produto (id, codigo, codigo_barras, nome, categoria, unidade, preco_compra, preco_venda, iva, stock_actual, stock_minimo, servico, ativo) VALUES (1, 'P-0001', '1234567890123', 'Tubo PEAD 20mm', 'CANALIZACAO', 'm', 120.00, 165.00, 16, 200, 10, FALSE, TRUE), (2, 'P-0002', '1234567890124', 'Junta de compressão 20mm', 'CANALIZACAO', 'UN', 45.00, 65.00, 16, 150, 5, FALSE, TRUE), (3, 'P-0003', '1234567890125', 'Torneira de esfera 1/2\"', 'CANALIZACAO', 'UN', 180.00, 250.00, 16, 40, 5, FALSE, TRUE), (4, 'S-0001', '1234567890126', 'Ligação de água (serviço)', 'SERVICO', 'UN', 0, 750.00, 16, 0, 0, TRUE, TRUE)");
        st.executeUpdate("INSERT INTO stock_item (produto_id, armazem_id, quantidade, stock_minimo, stock_maximo, estado) VALUES (1, 1, 200, 20, 400, 'OK'), (2, 1, 150, 10, 300, 'OK'), (3, 1, 40, 5, 100, 'OK'), (1, 2, 50, 10, 100, 'OK'), (2, 2, 30, 5, 60, 'OK')");
        st.executeUpdate("INSERT INTO fornecedor (id, codigo, nome, nuit, telefone, cidade, ativo, saldo) VALUES (1, 'F-0001', 'AquaMateriais, Lda', '900112233', '+258 84 111 2222', 'Maputo', TRUE, 0)");
        String mes = java.time.YearMonth.now().toString();
        st.executeUpdate("INSERT INTO producao (id, data_, periodo, volume_produzido, volume_facturado, perdas, nrw_percent, fonte) VALUES (1, CURRENT_DATE, '" + mes + "', 120000, 96000, 24000, 20.00, 'Captação principal')");
    }
}
