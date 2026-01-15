package be.appify.prefab.core.spring;

import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.ApplicationContext;
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

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfiguration.class);
    private static final Bindable<Map<String, OAuth2ClientProperties.Registration>> STRING_REGISTRATION_MAP = Bindable
            .mapOf(String.class, OAuth2ClientProperties.Registration.class);

    private final Map<String, OAuth2ClientProperties.Registration> registrations;

    WebSecurityConfiguration(ApplicationContext applicationContext) {
        var environment = applicationContext.getEnvironment();
        this.registrations = Binder.get(environment)
                .bind("spring.security.oauth2.client.registration", STRING_REGISTRATION_MAP)
                .orElse(Collections.emptyMap());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var security = http.csrf(CsrfConfigurer::disable);
        if (!registrations.isEmpty()) {
            security.authorizeHttpRequests(auth ->
                            auth.anyRequest().authenticated())
                    .oauth2Login(withDefaults());
        } else {
            log.warn("No OAuth2 client registration properties found. All security is disabled!");
        }
        return security.build();
    }
}
