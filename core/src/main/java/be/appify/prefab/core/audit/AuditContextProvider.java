package be.appify.prefab.core.audit;

/**
 * Provides the identity of the currently authenticated principal for audit trail population.
 * <p>
 * Implement this interface to supply the current user identity from your authentication mechanism,
 * e.g. by reading from Spring Security's {@code SecurityContextHolder}.
 * </p>
 * <p>
 * A default no-op implementation {@link SystemAuditContextProvider} is registered automatically
 * when no custom bean is present.
 * </p>
 */
public interface AuditContextProvider {

    /**
     * Returns the identifier of the currently authenticated user.
     *
     * @return the current user identifier; must not be {@code null}
     */
    String currentUserId();
}
