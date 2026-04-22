package be.appify.prefab.postgres.spring;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Sets {@code spring.datasource.driver-class-name} to {@code org.postgresql.Driver} as a default,
 * so that projects depending on {@code prefab-postgres} do not need to configure it manually.
 *
 * <p>A value explicitly set in {@code application.yml} or any other higher-priority source always
 * takes precedence because this processor inserts a property source at the lowest-priority position
 * in the environment.</p>
 */
public class PostgresDriverEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "prefabPostgresDefaults";
    private static final String DRIVER_CLASS_NAME_KEY = "spring.datasource.driver-class-name";
    private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        var defaults = Map.<String, Object>of(DRIVER_CLASS_NAME_KEY, POSTGRES_DRIVER_CLASS);
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
}

