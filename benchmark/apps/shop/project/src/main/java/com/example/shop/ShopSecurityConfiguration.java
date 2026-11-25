package com.example.shop;

import be.appify.prefab.core.security.HttpSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Gives the local and integration-test shop a credential-free authentication mechanism.
 *
 * <p>Spring Boot supplies a generated development user password when the application starts; no OAuth credentials
 * or static secrets are needed. Production profiles retain Prefab's default JWT resource-server configuration.</p>
 */
@Configuration
class ShopSecurityConfiguration {

    @Bean
    HttpSecurityCustomizer publicCategoryReads() {
        return http -> http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/categories").permitAll());
    }

    @Bean
    @Profile({"local", "test"})
    HttpSecurityCustomizer localAuthentication() {
        return http -> http
                .oauth2ResourceServer(AbstractHttpConfigurer::disable)
                .httpBasic(withDefaults());
    }
}
