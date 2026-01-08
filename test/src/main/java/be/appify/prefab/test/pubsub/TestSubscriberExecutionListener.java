package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static org.springframework.util.ReflectionUtils.setField;

/**
 * Test execution listener that injects Pub/Sub subscribers into test fields annotated with {@link TestSubscriber}.
 */
public class TestSubscriberExecutionListener extends AbstractTestExecutionListener {
    private final Map<Field, Subscriber<?>> subscriberByField = new HashMap<>();
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
                    .filter(field -> field.getType().isAssignableFrom(Subscriber.class) && field.isAnnotationPresent(
                            TestSubscriber.class))
                    .map(field -> new TestSubscriberField(field, field.getAnnotation(TestSubscriber.class)))
                    .forEach(testSubscriberField ->
                            injectTestSubscriber(testSubscriberField, testContext.getTestInstance()));
            subscriberByField.values().forEach(Subscriber::reset);
        });
    }

    private void injectTestSubscriber(TestSubscriberField testSubscriberField, Object testInstance) {
        var subscriber = subscriberByField.computeIfAbsent(testSubscriberField.field(), field -> {
            var topic = environment.resolvePlaceholders(testSubscriberField.annotation.topic());
            var subscriptionName = subscriptionNameFor(testInstance);
            return createSubscriber(subscriptionName, topic, field);
        });
        ReflectionUtils.makeAccessible(testSubscriberField.field());
        setField(testSubscriberField.field(), testInstance, subscriber);
    }

    private static String subscriptionNameFor(Object testInstance) {
        return toKebabCase(testInstance.getClass().getSimpleName());
    }

    private <T> Subscriber<T> createSubscriber(
            String subscriptionName,
            String topic,
            Field field
    ) {
        var messages = new ArrayList<T>();
        pubSubUtil.subscribe(topic, subscriptionName,
                (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], messages::add);
        return new Subscriber<>(messages);
    }

    private record TestSubscriberField(Field field, TestSubscriber annotation) {
    }
}
