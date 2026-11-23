package com.example.blog;

import be.appify.prefab.core.security.HttpSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Allows anonymous reads while leaving Prefab's authenticated fallback in place for writes.
 */
@Configuration
class PublicArticlesSecurity {

    @Bean
    HttpSecurityCustomizer publicArticleReads() {
        return http -> http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/articles", "/articles/*").permitAll());
    }
}
