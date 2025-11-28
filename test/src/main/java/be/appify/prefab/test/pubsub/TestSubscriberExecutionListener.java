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
import java.util.List;
import java.util.Map;

import static org.springframework.util.ReflectionUtils.setField;

public class TestSubscriberExecutionListener extends AbstractTestExecutionListener {
    private final Map<Field, List<?>> subscriberByField = new HashMap<>();
    private Environment environment;
    private PubSubUtil pubSubUtil;

    @Override
    public void prepareTestInstance(TestContext testContext) {
        environment = testContext.getApplicationContext().getBean(Environment.class);
        testContext.getApplicationContext().getBeanProvider(PubSubUtil.class).ifAvailable(pubSubUtil -> {
            this.pubSubUtil = pubSubUtil;
            Arrays.stream(testContext.getTestClass().getDeclaredFields())
                    .filter(field -> field.getType().isAssignableFrom(List.class) && field.isAnnotationPresent(
                            TestSubscriber.class))
                    .map(field -> new TestSubscriberField(field, field.getAnnotation(TestSubscriber.class)))
                    .forEach(testSubscriberField ->
                            injectTestSubscriber(testSubscriberField, testContext.getTestInstance()));
            subscriberByField.values().forEach(List::clear);
        });
    }

    @Override
    public void afterTestExecution(TestContext testContext) {
        if (pubSubUtil != null) {
            var subscriptionName = testContext.getTestInstance().getClass().getSimpleName();
            pubSubUtil.deleteSubscription(subscriptionName);
            subscriberByField.clear();
        }
    }

    private void injectTestSubscriber(TestSubscriberField testSubscriberField, Object testInstance) {
        var subscriber = subscriberByField.computeIfAbsent(testSubscriberField.field(), field -> {
            var topic = environment.resolvePlaceholders(testSubscriberField.annotation.topic());
            var subscriptionName = testInstance.getClass().getSimpleName();
            return createSubscriber(subscriptionName, topic, field);
        });
        ReflectionUtils.makeAccessible(testSubscriberField.field());
        setField(testSubscriberField.field(), testInstance, subscriber);
    }

    private <T> List<T> createSubscriber(
            String subscriptionName,
            String topic,
            Field field
    ) {
        var subscriber = new ArrayList<T>();
        pubSubUtil.subscribe(topic, subscriptionName,
                (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], subscriber::add);
        return subscriber;
    }

    private record TestSubscriberField(Field field, TestSubscriber annotation) {
    }
}
