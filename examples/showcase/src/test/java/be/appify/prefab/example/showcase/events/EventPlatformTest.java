package be.appify.prefab.example.showcase.events;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Event.Platform;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No event of the showcase names a messaging platform. That is what lets one source publish to Kafka, Pub/Sub or
 * SNS/SQS: Prefab derives the platform from the technology on the classpath.
 */
class EventPlatformTest {

    private static final String SHOWCASE_PACKAGE = "be.appify.prefab.example.showcase";

    @Test
    void everyEvent_derivesItsPlatformFromTheClasspath() throws ClassNotFoundException {
        var events = eventTypes();

        assertThat(events).as("event types found").isNotEmpty();
        for (var type : events) {
            assertThat(Arrays.stream(type.getAnnotationsByType(Event.class)))
                    .as("events declared on %s", type.getSimpleName())
                    .allSatisfy(event -> assertThat(event.platform()).isEqualTo(Platform.DERIVED));
        }
    }

    private static Set<Class<?>> eventTypes() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition
            ) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(Event.class));
        var types = new java.util.LinkedHashSet<Class<?>>();
        for (BeanDefinition definition : scanner.findCandidateComponents(SHOWCASE_PACKAGE)) {
            types.add(Class.forName(definition.getBeanClassName()));
        }
        return types;
    }
}
