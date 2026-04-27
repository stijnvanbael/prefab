package be.appify.prefab.test.persistence;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Sets test datasource defaults for PostgreSQL when {@code prefab-postgres} (Spring Data JDBC) is on the classpath.
 *
 * <p>The following defaults are applied when no explicit value is already present:</p>
 * <ul>
 *   <li>{@code spring.datasource.url} —
 *       {@code jdbc:tc:postgresql:18.3:///<app>?TC_REUSABLE=true&currentSchema=<app>},
 *       where {@code <app>} is the value of {@code spring.application.name} converted to lowercase.</li>
 *   <li>{@code spring.datasource.driver-class-name} —
 *       {@code org.testcontainers.jdbc.ContainerDatabaseDriver}</li>
 * </ul>
 *
 * <p>Any value supplied by the developer in {@code application-test.yml} or any other higher-priority
 * source takes precedence because this processor inserts a property source at the lowest-priority position
 * in the environment.</p>
 *
 * <p>This processor is only active when both Spring Data Relational
 * ({@code spring-boot-starter-data-jdbc}) and the Testcontainers JDBC URL driver
 * ({@code org.testcontainers.jdbc.ContainerDatabaseDriver}) are on the classpath.</p>
 */
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

