package be.appify.prefab.core.tenant;

/**
 * Resolves the current tenant identifier for the ongoing request.
 *
 * <p>Implement and register this interface as a Spring bean to enable multi-tenancy.  The tenant ID is
 * typically sourced from a JWT claim, a request header, or a thread-local set by a security filter.</p>
 *
 * <p>A default no-op implementation is auto-configured by
 * {@link TenantConfiguration} when no custom bean is present.  The no-op returns {@code null}, which
 * disables tenant filtering (all tenants' data is visible).</p>
 *
 * <p>Example – Spring Security JWT integration:</p>
 * <pre>{@code
 * @Component
 * public class JwtTenantContextProvider implements TenantContextProvider {
 *
 *     @Override
 *     public String currentTenantId() {
 *         var authentication = SecurityContextHolder.getContext().getAuthentication();
 *         if (authentication instanceof JwtAuthenticationToken jwt) {
 *             return jwt.getToken().getClaimAsString("organisation_id");
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 */
public interface TenantContextProvider {

    /**
     * Returns the current tenant identifier, or {@code null} if tenant filtering should be disabled.
     *
     * @return the tenant ID for the current request, or {@code null}
     */
    String currentTenantId();
}
