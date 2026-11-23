package be.appify.prefab.test.inmemory;

import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Forces {@code prefab.test.containers=true} with the highest precedence.
 * <p>
 * A record without components, so every instance is equal and test classes that opt into containers share one cached
 * context, separate from those that do not.
 */
record ContainersModeContextCustomizer() implements ContextCustomizer {

    private static final String PROPERTY_SOURCE_NAME = "prefabTestContainersMode";

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(TestInfrastructureMode.PROPERTY, "true")));
    }
}
