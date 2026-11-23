package com.example.legal;

import be.appify.prefab.core.audit.AuditContextProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityAuditContextProvider implements AuditContextProvider {
    @Override
    public String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
