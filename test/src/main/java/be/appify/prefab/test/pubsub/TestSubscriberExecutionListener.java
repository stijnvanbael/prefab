package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.TestEventConsumer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static org.springframework.util.ReflectionUtils.setField;

/**
 * Test execution listener that injects Pub/Sub subscribers into test fields annotated with {@link TestEventConsumer}.
 */
public class TestSubscriberExecutionListener extends AbstractTestExecutionListener {
    private final Map<Field, EventConsumer<?>> eventConsumerByField = new HashMap<>();
    private Environment environment;
    private PubSubUtil pubSubUtil;

    /** Constructs a new TestSubscriberExecutionListener. */
    public TestSubscriberExecutionListener() {
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        environment = testContext.getApplicationContext().getBean(Environment.class);
        testContext.getApplicationContext().getBeanProvider(PubSubUtil.class).ifAvailable(pubSubUtil -> {
            this.pubSubUtil = pubSubUtil;
            Arrays.stream(testContext.getTestClass().getDeclaredFields())
                    .filter(field -> field.getType().isAssignableFrom(EventConsumer.class)
                            && AnnotationUtils.getAnnotation(field, TestEventConsumer.class) != null)
                    .forEach(field -> injectEventConsumer(field, testContext.getTestInstance()));
            eventConsumerByField.values().forEach(EventConsumer::reset);
        });
    }

    private void injectEventConsumer(Field field, Object testInstance) {
        var consumer = eventConsumerByField.computeIfAbsent(field, f -> {
            var annotation = AnnotationUtils.getAnnotation(f, TestEventConsumer.class);
            var topic = environment.resolvePlaceholders(annotation.topic());
            var subscriptionName = subscriptionNameFor(testInstance);
            return createEventConsumer(subscriptionName, topic, f);
        });
        ReflectionUtils.makeAccessible(field);
        setField(field, testInstance, consumer);
    }

    private static String subscriptionNameFor(Object testInstance) {
        return toKebabCase(testInstance.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private <T> EventConsumer<T> createEventConsumer(String subscriptionName, String topic, Field field) {
        var consumer = new EventConsumer<T>();
        pubSubUtil.subscribe(topic, subscriptionName,
                (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0],
                consumer.messages()::add);
        return consumer;
    }
}

