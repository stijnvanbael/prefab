package be.appify.prefab.core.spring;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.pubsub.PubSubConfiguration;
import be.appify.prefab.core.sns.SnsConfiguration;
import be.appify.prefab.core.tenant.TenantConfiguration;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CoreAutoConfigurationActivationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    static Stream<Class<?>> configurations() {
        return Stream.of(
                AuditConfiguration.class,
                TenantConfiguration.class,
                PubSubConfiguration.class,
                SnsConfiguration.class);
    }

    static Stream<Class<?>> standaloneConfigurations() {
        return Stream.of(AuditConfiguration.class, TenantConfiguration.class);
    }

    static Stream<Arguments> configurationsWithMissingClass() {
        return Stream.of(
                Arguments.of(PubSubConfiguration.class, PubSubAdmin.class),
                Arguments.of(SnsConfiguration.class, SnsTemplate.class));
    }

    @ParameterizedTest
    @MethodSource("configurations")
    @DisplayName("does not activate without PrefabCoreConfiguration")
    void configuration_withoutPrefabCore_isInactive(Class<?> configuration) {
        runner.withConfiguration(AutoConfigurations.of(configuration))
                .run(context -> assertThat(context).doesNotHaveBean(configuration));
    }

    @ParameterizedTest
    @MethodSource("standaloneConfigurations")
    @DisplayName("activates when PrefabCoreConfiguration is present")
    void configuration_withPrefabCore_isActive(Class<?> configuration) {
        runner.withUserConfiguration(CoreMarker.class)
                .withConfiguration(AutoConfigurations.of(configuration))
                .run(context -> assertThat(context).hasSingleBean(configuration));
    }

    @Test
    @DisplayName("registry configuration does not activate without an EventRegistryCustomizer")
    void registryConfiguration_withoutCustomizer_isInactive() {
        runner.withConfiguration(AutoConfigurations.of(PrefabRegistryConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(PrefabRegistryConfiguration.class));
    }

    @Test
    @DisplayName("registry configuration activates when an EventRegistryCustomizer is present")
    void registryConfiguration_withCustomizer_isActive() {
        runner.withBean(EventRegistryCustomizer.class, () -> registry -> { })
                .withConfiguration(AutoConfigurations.of(PrefabRegistryConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(PrefabRegistryConfiguration.class)
                        .hasSingleBean(EventRegistry.class));
    }

    @ParameterizedTest
    @MethodSource("configurationsWithMissingClass")
    @DisplayName("does not activate when its library is not on the classpath")
    void configuration_withoutLibrary_isInactive(Class<?> configuration, Class<?> hiddenClass) {
        runner.withUserConfiguration(CoreMarker.class)
                .withClassLoader(new FilteredClassLoader(hiddenClass))
                .withConfiguration(AutoConfigurations.of(configuration))
                .run(context -> assertThat(context).doesNotHaveBean(configuration));
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreMarker {
        @Bean
        PrefabCoreConfiguration prefabCoreConfiguration() {
            return new PrefabCoreConfiguration();
        }
    }
}
