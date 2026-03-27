package be.appify.prefab.test.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.TestEventConsumer;
import org.springframework.core.annotation.AnnotationUtils;
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
 * Test execution listener that injects SQS subscribers into test fields annotated with {@link TestSqsSubscriber}
 * or {@link TestEventConsumer}.
 */
public class TestSqsSubscriberExecutionListener extends AbstractTestExecutionListener {
    private final Map<Field, SqsSubscriber<?>> subscriberByField = new HashMap<>();
    private final Map<Field, EventConsumer<?>> eventConsumerByField = new HashMap<>();
    private Environment environment;
    private SqsUtil sqsUtil;

    /** Constructs a new TestSqsSubscriberExecutionListener. */
    public TestSqsSubscriberExecutionListener() {
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        environment = testContext.getApplicationContext().getBean(Environment.class);
        testContext.getApplicationContext().getBeanProvider(SqsUtil.class).ifAvailable(sqsUtil -> {
            this.sqsUtil = sqsUtil;
            Arrays.stream(testContext.getTestClass().getDeclaredFields())
                    .filter(field -> field.getType().isAssignableFrom(SqsSubscriber.class)
                            && field.isAnnotationPresent(TestSqsSubscriber.class))
                    .map(field -> new TestSqsSubscriberField(field, field.getAnnotation(TestSqsSubscriber.class)))
                    .forEach(testSubscriberField ->
                            injectTestSubscriber(testSubscriberField, testContext.getTestInstance()));
            subscriberByField.values().forEach(SqsSubscriber::reset);

            Arrays.stream(testContext.getTestClass().getDeclaredFields())
                    .filter(field -> field.getType().isAssignableFrom(EventConsumer.class)
                            && AnnotationUtils.getAnnotation(field, TestEventConsumer.class) != null)
                    .forEach(field -> injectEventConsumer(field, testContext.getTestInstance()));
            eventConsumerByField.values().forEach(EventConsumer::reset);
        });
    }

    private void injectTestSubscriber(TestSqsSubscriberField testSubscriberField, Object testInstance) {
        var subscriber = subscriberByField.computeIfAbsent(testSubscriberField.field(), field -> {
            var topic = environment.resolvePlaceholders(testSubscriberField.annotation().topic());
            var queueName = queueNameFor(testInstance, topic);
            return createSubscriber(queueName, topic, field);
        });
        ReflectionUtils.makeAccessible(testSubscriberField.field());
        setField(testSubscriberField.field(), testInstance, subscriber);
    }

    private void injectEventConsumer(Field field, Object testInstance) {
        var consumer = eventConsumerByField.computeIfAbsent(field, f -> {
            var annotation = AnnotationUtils.getAnnotation(f, TestEventConsumer.class);
            var topic = environment.resolvePlaceholders(annotation.topic());
            var queueName = queueNameFor(testInstance, topic);
            return createEventConsumer(queueName, topic, f);
        });
        ReflectionUtils.makeAccessible(field);
        setField(field, testInstance, consumer);
    }

    private static String queueNameFor(Object testInstance, String topic) {
        return toKebabCase(testInstance.getClass().getSimpleName()) + "-" + topic;
    }

    @SuppressWarnings("unchecked")
    private <T> SqsSubscriber<T> createSubscriber(String queueName, String topic, Field field) {
        var messages = new ArrayList<T>();
        var type = (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        sqsUtil.subscribe(new SqsSubscriptionRequest<>(topic, queueName, type, messages::add)
                .withDeadLetterQueueName(null));
        return new SqsSubscriber<>(messages);
    }

    @SuppressWarnings("unchecked")
    private <T> EventConsumer<T> createEventConsumer(String queueName, String topic, Field field) {
        var consumer = new EventConsumer<T>();
        var type = (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        sqsUtil.subscribe(new SqsSubscriptionRequest<>(topic, queueName, type, consumer.messages()::add)
                .withDeadLetterQueueName(null));
        return consumer;
    }

    private record TestSqsSubscriberField(Field field, TestSqsSubscriber annotation) {
    }
}

