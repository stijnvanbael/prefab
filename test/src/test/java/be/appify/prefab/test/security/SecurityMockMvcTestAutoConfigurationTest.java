package be.appify.prefab.test.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityMockMvcTestAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void securityMockMvcConfigurerIsAbsentWithoutSecurityFilterChainBean() {
        contextRunner.withUserConfiguration(SecurityMockMvcTestAutoConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean("securityMockMvcConfigurer"));
    }

    @Test
    void securityMockMvcConfigurerIsRegisteredWhenFilterChainBeanIsPresent() {
        contextRunner.withUserConfiguration(FilterChainConfiguration.class, SecurityMockMvcTestAutoConfiguration.class)
                .run(context -> assertThat(context).hasBean("securityMockMvcConfigurer"));
    }

    @Test
    void userSuppliedConfigurerOverridesTheDefault() {
        contextRunner.withUserConfiguration(FilterChainConfiguration.class, CustomConfigurerConfiguration.class,
                        SecurityMockMvcTestAutoConfiguration.class)
                .run(context -> assertThat(context.getBean("securityMockMvcConfigurer"))
                        .isSameAs(context.getBean(CustomConfigurerConfiguration.class).customConfigurer));
    }

    @Configuration
    static class FilterChainConfiguration {
        @Bean
        String springSecurityFilterChain() {
            return "filter-chain-placeholder";
        }
    }

    @Configuration
    static class CustomConfigurerConfiguration {
        final MockMvcConfigurer customConfigurer = new MockMvcConfigurer() {
        };

        @Bean("securityMockMvcConfigurer")
        MockMvcConfigurer securityMockMvcConfigurer() {
            return customConfigurer;
        }
    }
}
