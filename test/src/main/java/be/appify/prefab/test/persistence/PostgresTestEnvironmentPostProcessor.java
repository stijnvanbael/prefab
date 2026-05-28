package be.appify.prefab.test.persistence;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Deprecated environment post-processor for PostgreSQL test configuration.
 *
 * <p>This class is deprecated in favor of {@link PostgresTestAutoConfiguration}, which provides
 * a programmatic bean-based approach with full support for fixed container names and reuse.
 *
 * <p>This processor now only acts as a fallback for projects that haven't yet migrated to using
 * the auto-configuration. If the datasource URL is not already configured, it will generate
 * a default JDBC URL using the legacy TC_REUSABLE approach. New projects should use
 * {@link PostgresTestAutoConfiguration} instead.
 *
 * @deprecated Use {@link PostgresTestAutoConfiguration} instead for fixed container names and full reuse support.
 */
@Deprecated(since = "0.9.0", forRemoval = true)
public class PostgresTestEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "prefabPostgresTestDefaults";
    private static final String URL_KEY = "spring.datasource.url";
    private static final String DRIVER_KEY = "spring.datasource.driver-class-name";
    private static final String TC_DRIVER = "org.testcontainers.jdbc.ContainerDatabaseDriver";
    private static final String SPRING_DATA_JDBC_CLASS = "org.springframework.data.relational.core.mapping.Table";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isSpringDataJdbcPresent() || !isTcDriverPresent()) {
            return;
        }
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        // Only apply legacy defaults if no datasource URL is configured
        // The new PostgresTestAutoConfiguration bean will handle the modern approach
        if (environment.getProperty(URL_KEY) != null) {
            return;
        }
        var appName = resolveApplicationName(environment);
        var defaults = Map.<String, Object>of(
                URL_KEY, tcUrl(appName),
                DRIVER_KEY, TC_DRIVER
        );
        var source = new MapPropertySource(PROPERTY_SOURCE_NAME, defaults);
        if (environment.getPropertySources().contains("prefabPostgresDefaults")) {
            environment.getPropertySources().addBefore("prefabPostgresDefaults", source);
        } else {
            environment.getPropertySources().addLast(source);
        }
    }

    private String resolveApplicationName(ConfigurableEnvironment environment) {
        var name = environment.getProperty("spring.application.name", "application");
        return name.toLowerCase().replaceAll("[.\\-]", "_");
    }

    private String tcUrl(String appName) {
        return "jdbc:tc:postgresql:18.3:///" + appName + "?TC_REUSABLE=true&currentSchema=" + appName;
    }

    private boolean isSpringDataJdbcPresent() {
        return isClassPresent(SPRING_DATA_JDBC_CLASS);
    }

    private boolean isTcDriverPresent() {
        return isClassPresent(TC_DRIVER);
    }

    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

