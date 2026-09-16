package be.appify.prefab.core.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Callback interface for contributing additional {@link HttpSecurity} configuration to
 * Prefab's default {@link WebSecurityConfiguration}.
 *
 * <p>Register one or more beans of this type to extend the default filter-chain setup without
 * replacing the framework-provided {@code SecurityFilterChain} bean.
 */
@FunctionalInterface
public interface HttpSecurityCustomizer {

    /**
     * Customize the shared {@link HttpSecurity} builder before the filter chain is built.
     *
     * @param http the builder used by Prefab's default security filter chain
     * @throws Exception if the customizer cannot apply its configuration
     */
    void customize(HttpSecurity http) throws Exception;
}
