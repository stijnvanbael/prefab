package be.appify.prefab.test.inmemory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

/**
 * Logs local application URLs when the test-scoped in-memory backend runs a web application.
 */
@AutoConfiguration
@Conditional(InMemoryTestAutoConfiguration.InMemoryModeCondition.class)
public class InMemoryLocalRunAutoConfiguration {

    /** Constructs a new in-memory local-run auto-configuration. */
    public InMemoryLocalRunAutoConfiguration() {
    }

    @Bean
    LocalRunStartupLogger localRunStartupLogger(Environment environment) {
        return new LocalRunStartupLogger(environment);
    }
}
