package com.sgv;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base integration test class.
 * Spins up a real MariaDB via Testcontainers and runs the full Spring context.
 *
 * Usage:
 *   @SpringBootTest(webEnvironment = WebEnvironment.NONE)
 *   @Testcontainers
 *   public class MyTest extends BaseIntegrationTest { ... }
 *
 * Database is shared across all tests in the class via JUnit 5 shared container.
 */
@SpringBootTest(
    properties = {
        "spring.main.web-application-type=none",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
        .withDatabaseName("sgv_test")
        .withUsername("test")
        .withPassword("test")
        .withSharedMemorySize(256L * 1024 * 1024);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.datasource.driver-class-name",
            () -> "org.mariadb.jdbc.Driver");
    }
}
