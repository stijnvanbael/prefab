package be.appify.prefab.core.audit;

/**
 * Default {@link AuditContextProvider} that returns {@code "system"} as the current user identifier.
 * <p>
 * This implementation is used when no custom {@link AuditContextProvider} bean is defined.
 * Replace it by declaring your own {@link AuditContextProvider} bean, for example reading the
 * authenticated principal from Spring Security's {@code SecurityContextHolder}.
 * </p>
 */
public class SystemAuditContextProvider implements AuditContextProvider {

    /** Constructs a new SystemAuditContextProvider. */
    public SystemAuditContextProvider() {
    }

    @Override
    public String currentUserId() {
        return "system";
    }
}
