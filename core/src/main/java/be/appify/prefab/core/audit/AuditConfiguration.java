package be.appify.prefab.core.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the audit trail infrastructure.
 * <p>
 * Registers a {@link SystemAuditContextProvider} bean when no custom {@link AuditContextProvider}
 * is present in the application context.
 * </p>
 * <p>
 * To supply the authenticated principal from Spring Security, declare your own
 * {@link AuditContextProvider} bean:
 * </p>
 * <pre>{@code
 * @Bean
 * public AuditContextProvider auditContextProvider() {
 *     return () -> SecurityContextHolder.getContext().getAuthentication().getName();
 * }
 * }</pre>
 */
@Configuration
public class AuditConfiguration {

    /** Constructs a new AuditConfiguration. */
    public AuditConfiguration() {
    }

    /**
     * Registers a no-op {@link AuditContextProvider} that returns {@code "system"} when no custom
     * implementation is present.
     *
     * @return the default {@link SystemAuditContextProvider}
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditContextProvider auditContextProvider() {
        return new SystemAuditContextProvider();
    }
}
