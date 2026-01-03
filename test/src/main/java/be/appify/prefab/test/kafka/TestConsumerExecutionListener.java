package be.appify.prefab.test.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test execution listener that injects Kafka consumers into test fields annotated with {@link TestConsumer}.
 */
public class TestConsumerExecutionListener extends AbstractTestExecutionListener {

    private static final Map<Field, Consumer<?, ?>> consumerByField = new HashMap<>();

    private Environment environment;

    private TestJsonTypeResolver typeResolver;

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
            this.consumerFactory = testContext.getApplicationContext().getBean("jsonTestConsumerFactory", ConsumerFactory.class);
            this.environment = testContext.getApplicationContext().getBean(Environment.class);
            this.typeResolver = testContext.getApplicationContext().getBean(TestJsonTypeResolver.class);
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
            consumer = createConsumer(testInstance, testConsumerField, topic);
            consumer.subscribe(List.of(topic));
            consumerByField.put(testConsumerField.field, consumer);
        }
        setField(testConsumerField.field, testInstance, consumer);
    }

    private Consumer<?, ?> createConsumer(Object testInstance, TestConsumerField field, String topic) {
        var type = field.field.getGenericType();
        if (type instanceof ParameterizedType parameterizedType) {
            var valueType = parameterizedType.getActualTypeArguments()[1];
            if (valueType instanceof Class<?> valueClass) {
                if (consumerFactory == null) {
                    throw new IllegalArgumentException("Json consumer factory not available, please add a " +
                            "ConsumerFactory bean to your context named `jsonTestConsumerFactory` capable of " +
                            "deserializing Json messages");
                }
                if (typeResolver == null) {
                    throw new IllegalArgumentException("Json type resolver not available, please add a " +
                            "TestJsonTypeResolver bean to your context capable of resolving the type of Json " +
                            "messages");
                }
                typeResolver.registerType(topic, valueClass);
                String id = testInstance.getClass().getSimpleName() + "-%s-json".formatted(index++);
                return consumerFactory.createConsumer(id, id);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported value type: " + valueType + " for field: " + field.field);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type: " + type + " for field: " + field.field + ". Expected: Consumer<K, V>");
        }
    }

    private void setField(Field field, Object testInstance, Consumer<?, ?> testConsumer) {
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, testInstance, testConsumer);
    }

    private record TestConsumerField(Field field, TestConsumer testConsumer) {
    }
}
