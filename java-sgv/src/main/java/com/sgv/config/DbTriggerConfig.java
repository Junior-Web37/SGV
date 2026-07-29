package com.sgv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DbTriggerConfig {

    private static final Logger log = LoggerFactory.getLogger(DbTriggerConfig.class);

    @Bean
    public CommandLineRunner initDatabaseTriggers(JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("Inicializando Triggers de proteção fiscal no MariaDB...");

            String databaseProductName = "unknown";
            if (jdbcTemplate.getDataSource() != null) {
                try (var connection = jdbcTemplate.getDataSource().getConnection()) {
                    databaseProductName = connection.getMetaData().getDatabaseProductName();
                }
            }

            if (databaseProductName == null || !databaseProductName.toLowerCase().contains("mysql") && !databaseProductName.toLowerCase().contains("mariadb")) {
                log.info("Skippando aplicação de triggers MariaDB para banco de dados: {}", databaseProductName);
                return;
            }

            try {
                // Trigger para impedir UPDATE de campos críticos em vendas emitidas/anuladas
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_prevent_sales_update");
                jdbcTemplate.execute(
                    "CREATE TRIGGER trg_prevent_sales_update " +
                    "BEFORE UPDATE ON sales " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "    IF OLD.state IN ('EMITIDA', 'ANULADA') THEN " +
                    "        IF OLD.total <> NEW.total OR " +
                    "           OLD.subtotal <> NEW.subtotal OR " +
                    "           OLD.total_tax <> NEW.total_tax OR " +
                    "           OLD.customer_nuit <> NEW.customer_nuit OR " +
                    "           OLD.document_number <> NEW.document_number OR " +
                    "           OLD.series <> NEW.series OR " +
                    "           OLD.document_type <> NEW.document_type THEN " +
                    "            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Documentos fiscais não podem ter os seus valores ou identificadores alterados após emissão.'; " +
                    "        END IF; " +
                    "    END IF; " +
                    "END;"
                );

                // Trigger para impedir DELETE de vendas emitidas/anuladas
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_prevent_sales_delete");
                jdbcTemplate.execute(
                    "CREATE TRIGGER trg_prevent_sales_delete " +
                    "BEFORE DELETE ON sales " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "    IF OLD.state IN ('EMITIDA', 'ANULADA') THEN " +
                    "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Documentos fiscais não podem ser apagados após emissão.'; " +
                    "    END IF; " +
                    "END;"
                );

                // Trigger para impedir UPDATE de itens de vendas emitidas/anuladas
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_prevent_sale_items_update");
                jdbcTemplate.execute(
                    "CREATE TRIGGER trg_prevent_sale_items_update " +
                    "BEFORE UPDATE ON sale_items " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "    DECLARE sale_state VARCHAR(255); " +
                    "    SELECT state INTO sale_state FROM sales WHERE id = OLD.sale_id; " +
                    "    IF sale_state IN ('EMITIDA', 'ANULADA') THEN " +
                    "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Itens de documentos fiscais emitidos não podem ser modificados.'; " +
                    "    END IF; " +
                    "END;"
                );

                // Trigger para impedir DELETE de itens de vendas emitidas/anuladas
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_prevent_sale_items_delete");
                jdbcTemplate.execute(
                    "CREATE TRIGGER trg_prevent_sale_items_delete " +
                    "BEFORE DELETE ON sale_items " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "    DECLARE sale_state VARCHAR(255); " +
                    "    SELECT state INTO sale_state FROM sales WHERE id = OLD.sale_id; " +
                    "    IF sale_state IN ('EMITIDA', 'ANULADA') THEN " +
                    "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Itens de documentos fiscais emitidos não podem ser apagados.'; " +
                    "    END IF; " +
                    "END;"
                );

                log.info("Triggers de proteção fiscal aplicados com sucesso.");
            } catch (Exception e) {
                log.error("Erro ao aplicar Triggers na Base de Dados. Verifique se está usando MariaDB/MySQL. Detalhes: " + e.getMessage());
                // Não quebramos o arranque caso a base de dados não esteja disponível ainda.
            }
        };
    }
}
