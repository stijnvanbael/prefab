package be.appify.prefab.asyncapi;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncApiConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AsyncApiConfiguration.class));

    @Test
    void configuration_withoutPrefabCore_isInactive() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AsyncApiConfiguration.class));
    }

    @Test
    void configuration_withPrefabCore_isActive() {
        runner.withUserConfiguration(CoreMarker.class)
                .run(context -> assertThat(context).hasSingleBean(AsyncApiConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreMarker {
        @Bean
        PrefabCoreConfiguration prefabCoreConfiguration() {
            return new PrefabCoreConfiguration();
        }
    }
}