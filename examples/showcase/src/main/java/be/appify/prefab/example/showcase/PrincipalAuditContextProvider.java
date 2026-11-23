package be.appify.prefab.example.showcase;

import be.appify.prefab.core.audit.AuditContextProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Uses the authenticated Spring Security principal as the audit user in the showcase.
 */
@Component
public class PrincipalAuditContextProvider implements AuditContextProvider {

    /** Returns the authenticated principal's name or {@code system} for unauthenticated operations. */
    @Override
    public String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }
}
