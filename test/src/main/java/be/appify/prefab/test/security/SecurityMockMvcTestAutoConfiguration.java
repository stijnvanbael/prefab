package be.appify.prefab.test.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;

/**
 * Autoconfiguration for Spring Security MockMvc test support.
 *
 * <p>When {@code spring-security-test} is on the classpath (i.e. when {@code prefab-test} is a dependency) and the
 * application context actually contributes a {@code springSecurityFilterChain} bean, this configuration registers
 * {@link SecurityMockMvcConfigurers#springSecurity()} as a regular {@link MockMvcConfigurer} bean named
 * {@code securityMockMvcConfigurer}.
 *
 * <p>Because it is a normal bean, generated test clients compose it with any other {@link MockMvcConfigurer} beans
 * contributed by the application instead of treating them as mutually exclusive. Adopters that need to customise the
 * security wiring (e.g. a custom {@code SecurityContextRepository}) can override it entirely by declaring their own
 * {@code @Bean("securityMockMvcConfigurer") MockMvcConfigurer securityMockMvcConfigurer()}.
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnClass(SecurityMockMvcConfigurers.class)
public class SecurityMockMvcTestAutoConfiguration {

    /**
     * Constructs a new SecurityMockMvcTestAutoConfiguration.
     */
    public SecurityMockMvcTestAutoConfiguration() {
    }

    /**
     * Registers the default Spring Security {@link MockMvcConfigurer} when a {@code springSecurityFilterChain} bean
     * is present in the application context.
     *
     * @return the default Spring Security MockMvc configurer
     */
    @Bean
    @ConditionalOnBean(name = "springSecurityFilterChain")
    @ConditionalOnMissingBean(name = "securityMockMvcConfigurer")
    MockMvcConfigurer securityMockMvcConfigurer() {
        return SecurityMockMvcConfigurers.springSecurity();
    }
}
