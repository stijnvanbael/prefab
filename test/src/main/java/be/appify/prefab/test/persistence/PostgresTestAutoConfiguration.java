package be.appify.prefab.test.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import be.appify.prefab.test.TestContainerNameResolver;

/**
 * Autoconfiguration for PostgreSQL test support.
 *
 * <p>Provides a programmatic PostgreSQL Testcontainer with support for fixed Docker names and reuse.
 * This replaces the legacy JDBC URL approach (TC_REUSABLE=true) with a full Spring bean that
 * integrates with Testcontainers' Docker name and reuse capabilities.
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnClass(Table.class)
public class PostgresTestAutoConfiguration {

    /**
     * Constructs a new PostgresTestAutoConfiguration.
     */
    public PostgresTestAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(name = "postgresContainer")
    PostgreSQLContainer postgresContainer(PropertyResolver propertyResolver) {
        var appName = propertyResolver.getProperty("spring.application.name", "application");
        var sanitisedName = appName.toLowerCase().replaceAll("[.\\-]", "_");
        var containerName = TestContainerNameResolver.resolveContainerName(
                propertyResolver, "postgres", "prefab.test.postgres.container-name");
        TestContainerNameResolver.removeConflictingContainer(containerName);
        var container = new PostgreSQLContainer("postgres:18.3-alpine")
                .withDatabaseName(sanitisedName)
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
    DynamicPropertyRegistrar postgresPropertiesRegistrar(PostgreSQLContainer postgresContainer) {
        return registry -> {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgresContainer::getUsername);
            registry.add("spring.datasource.password", postgresContainer::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        };
    }
}

