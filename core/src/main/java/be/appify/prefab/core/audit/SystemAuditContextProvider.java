package be.appify.prefab.core.audit;

/**
 * Default no-op implementation of {@link AuditContextProvider} that returns {@code "system"}.
 * <p>
 * This implementation is auto-configured and used when no custom {@link AuditContextProvider} bean
 * is defined. Replace it with an application-specific implementation to integrate with your
 * authentication provider (e.g. Spring Security).
 * </p>
 */
public class SystemAuditContextProvider implements AuditContextProvider {

    /** Constructs a new {@code SystemAuditContextProvider}. */
    public SystemAuditContextProvider() {
    }

    @Override
    public String currentUserId() {
        return "system";
    }
}
