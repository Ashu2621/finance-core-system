package com.finaxis.financecore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesOperationalAndGovernanceTables() {
        Integer migrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class
        );
        assertThat(migrations).isGreaterThanOrEqualTo(3);

        assertThat(tableExists("users")).isTrue();
        assertThat(tableExists("records")).isTrue();
        assertThat(tableExists("audit_events")).isTrue();
        assertThat(tableExists("idempotency_records")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from information_schema.tables
                    where table_schema = 'public' and table_name = ?
                )
                """,
                Boolean.class,
                tableName
        );
        return Boolean.TRUE.equals(exists);
    }
}
