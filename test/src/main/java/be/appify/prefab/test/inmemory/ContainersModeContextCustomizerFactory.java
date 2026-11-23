package be.appify.prefab.test.inmemory;

import be.appify.prefab.test.IntegrationTest;
import java.util.List;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * Creates a {@link ContextCustomizer} for test classes annotated with {@code @IntegrationTest(containers = true)},
 * directly, as a meta-annotation, or on an enclosing class of a {@code @Nested} test.
 */
public class ContainersModeContextCustomizerFactory implements ContextCustomizerFactory {

    /** Constructs a new ContainersModeContextCustomizerFactory. */
    public ContainersModeContextCustomizerFactory() {
    }

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes
    ) {
        var annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, IntegrationTest.class);
        return annotation != null && annotation.containers() ? new ContainersModeContextCustomizer() : null;
    }
}
