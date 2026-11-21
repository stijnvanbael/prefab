package com.example.tenancy;

import be.appify.prefab.core.tenant.TenantContextProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
