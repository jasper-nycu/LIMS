package com.tsmc.lims.backend;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.test.context.DynamicPropertyRegistry;

final class PostgresTestSupport {

    private static final String DB_NAME = valueOrDefault("DB_NAME", "lims_db");
    private static final String DB_USER = valueOrDefault("DB_USER", "lims_admin");
    private static final String DB_PASSWORD = valueOrDefault("DB_PASSWORD", "your_local_db_password_here");
    private static final String BASE_URL = "jdbc:postgresql://localhost:5432/" + DB_NAME;

    private PostgresTestSupport() {
    }

    static void configure(DynamicPropertyRegistry registry, String schemaName) {
        createSchema(schemaName);
        registry.add("spring.datasource.url", () -> BASE_URL + "?currentSchema=" + schemaName);
        registry.add("spring.datasource.username", () -> DB_USER);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> schemaName);
        registry.add("spring.sql.init.mode", () -> "never");
    }

    private static void createSchema(String schemaName) {
        try (var connection = DriverManager.getConnection(BASE_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to prepare PostgreSQL test schema: " + schemaName, exception);
        }
    }

    private static String valueOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
