package be.appify.prefab.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated())
                .oauth2Login(withDefaults())
                .build();
    }
}
