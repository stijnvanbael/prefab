package be.appify.prefab.core.view;

import java.security.Principal;
import java.util.Optional;

/**
 * Trusted request context available to a batch view assembler.
 *
 * @param currentUser authenticated request principal, when present
 * @param tenantId current tenant identifier, when present
 */
public record ViewContext(Optional<Principal> currentUser, Optional<String> tenantId) {
    /**
     * Creates a view context with normalized optional values.
     *
     * @param currentUser current authenticated principal, or {@code null}
     * @param tenantId current tenant ID, or {@code null}
     * @return an immutable context
     */
    public static ViewContext of(Principal currentUser, String tenantId) {
        return new ViewContext(Optional.ofNullable(currentUser), Optional.ofNullable(tenantId));
    }
}
