package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.TestEventConsumer;
import be.appify.prefab.test.inmemory.TestInfrastructureMode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * Injects {@link EventConsumer} fields annotated with {@link TestEventConsumer} in in-memory mode by subscribing
 * to the {@link InMemoryEventBus}. Received events are decoded like those of any other subscriber, and every test
 * starts with an empty consumer.
 */
public class InMemoryEventConsumerExecutionListener extends AbstractTestExecutionListener {
    private static final String SUBSCRIPTIONS = InMemoryEventConsumerExecutionListener.class.getName()
            + ".subscriptions";

    /** Constructs a new InMemoryEventConsumerExecutionListener. */
    public InMemoryEventConsumerExecutionListener() {
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        var context = testContext.getApplicationContext();
        if (!TestInfrastructureMode.inMemory(context.getEnvironment())) {
            return;
        }
        var fields = eventConsumerFields(testContext.getTestClass());
        if (fields.isEmpty()) {
            return;
        }
        var bus = context.getBean(InMemoryEventBus.class);
        var subscriptions = new ArrayList<InMemoryEventBus.Subscription>();
        fields.forEach(field -> subscriptions.add(inject(field, testContext, bus)));
        testContext.setAttribute(SUBSCRIPTIONS, subscriptions);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterTestMethod(TestContext testContext) {
        if (testContext.removeAttribute(SUBSCRIPTIONS) instanceof List<?> subscriptions) {
            ((List<InMemoryEventBus.Subscription>) subscriptions).forEach(InMemoryEventBus.Subscription::close);
        }
    }

    private static List<Field> eventConsumerFields(Class<?> testClass) {
        var fields = new ArrayList<Field>();
        ReflectionUtils.doWithFields(testClass, fields::add, field ->
                field.getType().isAssignableFrom(EventConsumer.class)
                        && AnnotationUtils.getAnnotation(field, TestEventConsumer.class) != null);
        return fields;
    }

    private static InMemoryEventBus.Subscription inject(Field field, TestContext testContext, InMemoryEventBus bus) {
        var annotation = AnnotationUtils.getAnnotation(field, TestEventConsumer.class);
        var topic = testContext.getApplicationContext().getEnvironment().resolvePlaceholders(annotation.topic());
        var consumer = new EventConsumer<Object>();
        var subscription = bus.subscribe(topic, "%s.%s".formatted(testContext.getTestClass().getSimpleName(),
                field.getName()), message -> consumer.addMessage(message.decode()));
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, testContext.getTestInstance(), consumer);
        return subscription;
    }
}
