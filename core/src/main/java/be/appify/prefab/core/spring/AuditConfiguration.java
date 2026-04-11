package be.appify.prefab.core.spring;

import be.appify.prefab.core.audit.AuditContextProvider;
import be.appify.prefab.core.audit.SystemAuditContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Prefab audit support.
 * <p>
 * Registers a default {@link SystemAuditContextProvider} bean that returns {@code "system"} as
 * the current user ID. Override this by declaring your own {@link AuditContextProvider} bean:
 * </p>
 * <pre>{@code
 * @Bean
 * public AuditContextProvider auditContextProvider() {
 *     return () -> SecurityContextHolder.getContext()
 *         .getAuthentication().getName();
 * }
 * }</pre>
 */
@Configuration
public class AuditConfiguration {

    /** Constructs a new {@code AuditConfiguration}. */
    public AuditConfiguration() {
    }

    /**
     * Registers the default {@link SystemAuditContextProvider} bean when no other
     * {@link AuditContextProvider} is present in the application context.
     *
     * @return the default audit context provider
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditContextProvider auditContextProvider() {
        return new SystemAuditContextProvider();
    }
}
