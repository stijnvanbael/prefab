package com.example.helpdesk;

import be.appify.prefab.core.tenant.TenantContextProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Takes the tenant from the {@code X-Tenant-Id} header that the API gateway sets. */
@Component
class HeaderTenantContextProvider implements TenantContextProvider {
    @Override
    public String currentTenantId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader("X-Tenant-Id");
        }
        return null;
    }
}
