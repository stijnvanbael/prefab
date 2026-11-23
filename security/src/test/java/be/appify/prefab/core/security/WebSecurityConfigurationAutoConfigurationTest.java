package be.appify.prefab.core.security;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigurationAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebSecurityConfiguration.class))
            .withInitializer(context -> context.getBeanFactory().registerSingleton(
                    "prefabCoreConfiguration", new PrefabCoreConfiguration()))
            .withUserConfiguration(AdditionalSecurityChainConfiguration.class);

    @Test
    void additionalPathScopedChainDoesNotRemovePrefabDefaultChain() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(SecurityFilterChain.class))
                    .containsKeys("prefabSecurityFilterChain", "publicEndpointSecurityFilterChain");

            var defaultChain = context.getBean("prefabSecurityFilterChain", SecurityFilterChain.class);
            assertThat(defaultChain.matches(new MockHttpServletRequest("GET", "/private"))).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class AdditionalSecurityChainConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .build();
        }

        @Bean
        @Order(1)
        SecurityFilterChain publicEndpointSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.securityMatcher("/public/**")
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
}
