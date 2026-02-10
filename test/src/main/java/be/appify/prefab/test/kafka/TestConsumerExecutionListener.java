package be.appify.prefab.test.kafka;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * Test execution listener that injects Kafka consumers into test fields annotated with {@link TestConsumer}.
 */
public class TestConsumerExecutionListener extends AbstractTestExecutionListener {

    private static final Map<Field, Consumer<?, ?>> consumerByField = new HashMap<>();

    private Environment environment;

    private ConsumerFactory<?, ?> consumerFactory;

    private int index = 0;

    /** Constructs a new TestConsumerExecutionListener. */
    public TestConsumerExecutionListener() {
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        var testConsumers = Arrays.stream(testContext.getTestClass().getDeclaredFields())
                .map(field -> new TestConsumerField(field,
                        AnnotationUtils.getAnnotation(field, TestConsumer.class)))
                .filter(testConsumer -> testConsumer.testConsumer != null)
                .toList();

        if (!testConsumers.isEmpty()) {
            this.consumerFactory = testContext.getApplicationContext().getBean("testConsumerFactory", ConsumerFactory.class);
            this.environment = testContext.getApplicationContext().getBean(Environment.class);
            injectTestConsumers(testConsumers, testContext.getTestInstance());
        }
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        var testClass = testContext.getTestClass();
        var fields = Arrays.asList(testClass.getDeclaredFields());

        var fieldsToRemove = consumerByField.entrySet().stream()
                .filter(entry -> fields.contains(entry.getKey()))
                .peek(entry -> entry.getValue().close())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        fieldsToRemove.forEach(consumerByField::remove);
    }

    private void injectTestConsumers(List<TestConsumerField> testConsumers, Object testInstance) {
        testConsumers.forEach(t -> injectTestConsumer(t, testInstance));
    }

    private void injectTestConsumer(TestConsumerField testConsumerField, Object testInstance) {
        var consumer = consumerByField.get(testConsumerField.field);
        if (consumer == null) {
            var topic = this.environment.resolvePlaceholders(testConsumerField.testConsumer.topic());
            consumer = createConsumer(testInstance);
            consumer.subscribe(List.of(topic));
            consumerByField.put(testConsumerField.field, consumer);
        }
        setField(testConsumerField.field, testInstance, consumer);
    }

    private Consumer<?, ?> createConsumer(Object testInstance) {
        if (consumerFactory == null) {
            throw new IllegalArgumentException("Test consumer factory not available, please add a " +
                    "ConsumerFactory bean to your context named `testConsumerFactory`");
        }
        String id = testInstance.getClass().getSimpleName() + "-%s-json".formatted(index++);
        return consumerFactory.createConsumer(id, id);
    }

    private void setField(Field field, Object testInstance, Consumer<?, ?> testConsumer) {
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, testInstance, testConsumer);
    }

    private record TestConsumerField(Field field, TestConsumer testConsumer) {
    }
}
