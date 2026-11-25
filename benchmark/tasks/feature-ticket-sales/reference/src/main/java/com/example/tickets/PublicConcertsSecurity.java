package com.example.tickets;

import be.appify.prefab.core.security.HttpSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Lets anonymous visitors browse concerts; everything else keeps Prefab's authenticated fallback. */
@Configuration
class PublicConcertsSecurity {

    @Bean
    HttpSecurityCustomizer publicConcertReads() {
        return http -> http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/concerts", "/concerts/*").permitAll());
    }
}
