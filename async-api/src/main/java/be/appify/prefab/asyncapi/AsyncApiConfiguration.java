package be.appify.prefab.asyncapi;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Prefab AsyncAPI documentation support.
 *
 * <p>Adds the module to a Spring Boot application by importing this configuration or by adding
 * {@code prefab-async-api} as a dependency (which triggers component scanning via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}).
 */
@Configuration
@ComponentScan
public class AsyncApiConfiguration {

    /** Constructs a new AsyncApiConfiguration. */
    public AsyncApiConfiguration() {
    }
}
