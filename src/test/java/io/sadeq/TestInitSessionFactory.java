package io.sadeq;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TestInitSessionFactory {
    @SuppressWarnings({"rawtypes", "resource"})
    @Container
    public static PostgreSQLContainer postgreSQLContainer =
            (PostgreSQLContainer) new PostgreSQLContainer("postgres:14.4-alpine")
                    .withDatabaseName("postgres")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("database/INIT.sql");

    @BeforeAll
    public static void init() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(Post.class)
                .setProperty("hibernate.connection.url", postgreSQLContainer.getJdbcUrl())
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.username", "postgres")
                .setProperty("hibernate.connection.password", "postgres")
                .setProperty("hibernate.hbm2ddl.auto", "update")
                .setProperty("hibernate.c3p0.min_size", "10")
                .setProperty("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider")
                .setProperty("hibernate.connection.autocommit", "false")
                .setProperty("hibernate.integration.envers.enabled", "true")
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.format_sql", "false")
                .setProperty("hibernate.use_sql_comments", "false");

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties());
        @SuppressWarnings("unused")
        SessionFactory sessionFactory = configuration.buildSessionFactory(builder.build());
    }

    @Test
    public void test() {
        // just an empty placeholder test
        Assertions.assertEquals(1, 1);
    }
}
