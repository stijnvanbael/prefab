package be.appify.prefab.openapi;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration.class));

    @Test
    void openApi_withoutPrefabCore_isNotRegistered() {
        runner.run(context -> assertThat(context).doesNotHaveBean(OpenAPI.class));
    }

    @Test
    void openApi_withPrefabCore_isRegistered() {
        runner.withUserConfiguration(CoreMarker.class)
                .run(context -> assertThat(context).hasSingleBean(OpenAPI.class));
    }

    @Test
    void openApi_withoutSwaggerModels_isNotRegistered() {
        runner.withUserConfiguration(CoreMarker.class)
                .withClassLoader(new FilteredClassLoader(OpenAPI.class))
                .run(context -> assertThat(context).doesNotHaveBean(OpenApiAutoConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreMarker {
        @Bean
        PrefabCoreConfiguration prefabCoreConfiguration() {
            return new PrefabCoreConfiguration();
        }
    }
}