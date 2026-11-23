package be.appify.prefab.test.inmemory;

import be.appify.prefab.test.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ContainersModeContextCustomizerFactoryTest {

    private final ContainersModeContextCustomizerFactory factory = new ContainersModeContextCustomizerFactory();

    @IntegrationTest(containers = true)
    static class WithContainers {
        @Nested
        class Inner {
        }
    }

    @IntegrationTest
    static class WithoutContainers {
    }

    @IntegrationTest(containers = true)
    static class OtherWithContainers {
    }

    static class NotAnIntegrationTest {
    }

    @Test
    void customizerIsCreatedWhenContainersAttributeIsTrue() {
        assertThat(factory.createContextCustomizer(WithContainers.class, List.of())).isNotNull();
    }

    @Test
    void customizerIsCreatedForNestedClassOfContainersTest() {
        assertThat(factory.createContextCustomizer(WithContainers.Inner.class, List.of())).isNotNull();
    }

    @Test
    void noCustomizerWithoutContainersAttribute() {
        assertThat(factory.createContextCustomizer(WithoutContainers.class, List.of())).isNull();
    }

    @Test
    void noCustomizerWithoutIntegrationTestAnnotation() {
        assertThat(factory.createContextCustomizer(NotAnIntegrationTest.class, List.of())).isNull();
    }

    @Test
    void customizersAreEqualSoContainersTestsShareACachedContext() {
        var first = factory.createContextCustomizer(WithContainers.class, List.of());
        var second = factory.createContextCustomizer(OtherWithContainers.class, List.of());

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void customizerOverridesConfiguredPropertyWithHighestPrecedence() {
        var context = new GenericApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("config", Map.of(TestInfrastructureMode.PROPERTY, "false")));

        factory.createContextCustomizer(WithContainers.class, List.of()).customizeContext(context, null);

        assertThat(TestInfrastructureMode.containersEnabled(context)).isTrue();
    }

    @Test
    void containersAreEnabledByDefault() {
        assertThat(TestInfrastructureMode.containersEnabled(new GenericApplicationContext())).isTrue();
    }
}
