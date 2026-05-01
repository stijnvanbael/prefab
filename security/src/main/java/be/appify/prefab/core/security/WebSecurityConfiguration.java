package be.appify.prefab.core.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configuration class for web security settings.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    WebSecurityConfiguration() {
    }

    /**
     * Configures a stateless OAuth2 security filter chain that:
     * <ul>
     *   <li>Disables CSRF — safe because {@link SessionCreationPolicy#STATELESS} prevents session cookies from being issued.</li>
     *   <li>Enforces {@code STATELESS} session management to prevent accidental session creation.</li>
     *   <li>Supports both browser OAuth2 login and programmatic API access via Bearer tokens.</li>
     *   <li>Adds restrictive security response headers (CSP, Referrer-Policy).</li>
     * </ul>
     * <p>
     * Adopters who embed a Swagger UI must loosen the {@code Content-Security-Policy} (e.g. allow {@code 'unsafe-inline'} for styles).
     * Override this bean with {@code @Bean @Primary} or rely on {@code @ConditionalOnMissingBean} to supply a custom chain.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(CsrfConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated())
                .oauth2Login(withDefaults())
                .oauth2ResourceServer(rs -> rs.jwt(withDefaults()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .build();
    }
}
