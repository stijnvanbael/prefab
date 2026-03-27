package be.appify.prefab.test.kafka;

import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.TestEventConsumer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
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
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Test execution listener that injects Kafka consumers into test fields annotated with {@link TestConsumer}
 * or {@link TestEventConsumer}.
 */
public class TestConsumerExecutionListener extends AbstractTestExecutionListener {

    private static final Map<Field, Consumer<?, ?>> consumerByField = new HashMap<>();
    private static final Map<Field, KafkaEventConsumerHolder<?>> eventConsumerByField = new HashMap<>();

    private Environment environment;

    private ConsumerFactory<?, ?> consumerFactory;

    private int index = 0;

    /** Constructs a new TestConsumerExecutionListener. */
    public TestConsumerExecutionListener() {
    }

    private static boolean isKafkaAvailable() {
        return ClassUtils.isPresent("org.springframework.kafka.core.ConsumerFactory",
                TestConsumerExecutionListener.class.getClassLoader());
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        if (!isKafkaAvailable()) {
            return;
        }
        var testConsumers = Arrays.stream(testContext.getTestClass().getDeclaredFields())
                .map(field -> new TestConsumerField(field,
                        AnnotationUtils.getAnnotation(field, TestConsumer.class)))
                .filter(testConsumer -> testConsumer.testConsumer != null)
                .toList();

        var eventConsumerFields = Arrays.stream(testContext.getTestClass().getDeclaredFields())
                .filter(field -> field.getType().isAssignableFrom(EventConsumer.class)
                        && AnnotationUtils.getAnnotation(field, TestEventConsumer.class) != null)
                .toList();

        if (!testConsumers.isEmpty() || !eventConsumerFields.isEmpty()) {
            this.consumerFactory = testContext.getApplicationContext().getBean("testConsumerFactory", ConsumerFactory.class);
            this.environment = testContext.getApplicationContext().getBean(Environment.class);
            injectTestConsumers(testConsumers, testContext.getTestInstance());
            eventConsumerFields.forEach(field -> injectEventConsumer(field, testContext.getTestInstance()));
        }
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        if (!isKafkaAvailable()) {
            return;
        }
        var testClass = testContext.getTestClass();
        var fields = Arrays.asList(testClass.getDeclaredFields());

        var fieldsToRemove = consumerByField.entrySet().stream()
                .filter(entry -> fields.contains(entry.getKey()))
                .peek(entry -> entry.getValue().close())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        fieldsToRemove.forEach(consumerByField::remove);

        var eventConsumerFieldsToRemove = eventConsumerByField.entrySet().stream()
                .filter(entry -> fields.contains(entry.getKey()))
                .peek(entry -> entry.getValue().stop())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        eventConsumerFieldsToRemove.forEach(eventConsumerByField::remove);
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

    @SuppressWarnings("unchecked")
    private <V> void injectEventConsumer(Field field, Object testInstance) {
        var holder = (KafkaEventConsumerHolder<V>) eventConsumerByField.computeIfAbsent(field, f -> {
            var annotation = AnnotationUtils.getAnnotation(f, TestEventConsumer.class);
            var topic = environment.resolvePlaceholders(annotation.topic());
            var kafkaConsumer = (Consumer<String, V>) createConsumer(testInstance);
            kafkaConsumer.subscribe(List.of(topic));
            var eventConsumer = new EventConsumer<V>();
            var pollingThread = Thread.ofVirtual().start(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        var records = kafkaConsumer.poll(Duration.ofMillis(100));
                        records.forEach(record -> eventConsumer.messages().add(record.value()));
                    } catch (Exception e) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    }
                }
            });
            return new KafkaEventConsumerHolder<>(kafkaConsumer, eventConsumer, pollingThread);
        });
        holder.eventConsumer().reset();
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, testInstance, holder.eventConsumer());
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

    private record KafkaEventConsumerHolder<V>(
            Consumer<String, V> kafkaConsumer,
            EventConsumer<V> eventConsumer,
            Thread pollingThread
    ) {
        void stop() {
            pollingThread.interrupt();
            try {
                pollingThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            kafkaConsumer.close();
        }
    }
}

