package be.appify.prefab.test.persistence;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import be.appify.prefab.test.TestContainerNameResolver;

/**
 * Autoconfiguration for PostgreSQL test support.
 *
 * <p>Provides a programmatic PostgreSQL Testcontainer with support for fixed Docker names and reuse.
 * This replaces the legacy JDBC URL approach (TC_REUSABLE=true) with a full Spring bean that
 * integrates with Testcontainers' Docker name and reuse capabilities.
 */
@AutoConfiguration
@ConditionalOnClass(Table.class)
@ConditionalOnBean(PrefabCoreConfiguration.class)
public class PostgresTestAutoConfiguration {
    private static final String POSTGRES_IMAGE = "postgres:18.6-alpine";

    /**
     * Constructs a new PostgresTestAutoConfiguration.
     */
    public PostgresTestAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(name = "postgresContainer")
    PostgreSQLContainer postgresContainer(PropertyResolver propertyResolver) {
        var containerName = TestContainerNameResolver.resolveSharedContainerName(
                propertyResolver, "postgres", "prefab.test.postgres.container-name");
        TestContainerNameResolver.removeStaleContainer(containerName, POSTGRES_IMAGE);
        var container = new PostgreSQLContainer(POSTGRES_IMAGE)
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(true)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
        if (!container.isRunning()) {
            container.start();
        }
        return container;
    }

    @Bean
    DynamicPropertyRegistrar postgresPropertiesRegistrar(
            PostgreSQLContainer postgresContainer,
            PropertyResolver propertyResolver
    ) {
        var database = TestContainerNameResolver.resolveApplicationIdentifier(propertyResolver);
        return registry -> {
            registry.add("spring.datasource.url", () -> applicationDatabaseUrl(postgresContainer, database));
            registry.add("spring.datasource.username", postgresContainer::getUsername);
            registry.add("spring.datasource.password", postgresContainer::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        };
    }

    /**
     * The container is shared by all applications, so every application gets its own database on it.
     */
    private static String applicationDatabaseUrl(PostgreSQLContainer container, String database) {
        try (var connection = DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
             var exists = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            exists.setString(1, database);
            if (!exists.executeQuery().next()) {
                connection.createStatement().execute("CREATE DATABASE \"%s\"".formatted(database));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create test database " + database, e);
        }
        return container.getJdbcUrl().replaceFirst("/" + container.getDatabaseName() + "(\\?|$)", "/" + database + "$1");
    }
}

