package be.appify.prefab.core.audit;

/**
 * Strategy interface for resolving the identifier of the currently authenticated principal.
 * <p>
 * Implement this interface as a Spring bean to integrate with your authentication mechanism. The
 * returned string is used to populate fields annotated with
 * {@link be.appify.prefab.core.annotations.audit.CreatedBy} and
 * {@link be.appify.prefab.core.annotations.audit.LastModifiedBy}.
 * </p>
 * <p>
 * A default no-op implementation ({@link SystemAuditContextProvider}) is registered automatically
 * and returns {@code "system"} for unauthenticated or system-initiated operations.
 * </p>
 *
 * <h2>Spring Security integration example</h2>
 * <pre>{@code
 * @Bean
 * public AuditContextProvider auditContextProvider() {
 *     return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
 *         .map(Authentication::getName)
 *         .orElse("anonymous");
 * }
 * }</pre>
 */
public interface AuditContextProvider {

    /**
     * Returns the identifier of the currently authenticated user.
     *
     * @return the user ID string, never {@code null}
     */
    String currentUserId();
}
