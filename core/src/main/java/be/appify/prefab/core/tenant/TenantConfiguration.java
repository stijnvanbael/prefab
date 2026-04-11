package be.appify.prefab.core.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers a no-op {@link TenantContextProvider} bean when no custom implementation
 * is present in the application context.
 *
 * <p>The no-op implementation returns {@code null} from {@link TenantContextProvider#currentTenantId()},
 * which disables tenant filtering so all tenants' data is visible.  A warning is logged at startup to alert
 * developers that multi-tenancy is not active.</p>
 *
 * <p>To enable multi-tenancy, provide a custom {@link TenantContextProvider} bean that resolves the tenant ID
 * from the incoming request (e.g. a JWT claim or a request header).</p>
 */
@AutoConfiguration
public class TenantConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantConfiguration.class);

    /** Constructs a new TenantConfiguration. */
    public TenantConfiguration() {
    }

    /**
     * Registers a no-op {@link TenantContextProvider} bean when no custom implementation is present.
     * Logs a warning to indicate that tenant filtering is disabled.
     *
     * @return a no-op {@link TenantContextProvider} that always returns {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantContextProvider tenantContextProvider() {
        log.warn("No TenantContextProvider bean found. Tenant filtering is disabled. "
                + "Implement TenantContextProvider and register it as a Spring bean to enable multi-tenancy.");
        return () -> null;
    }
}
