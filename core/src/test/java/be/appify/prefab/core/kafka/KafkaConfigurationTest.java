package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KafkaConfigurationTest {

    private final KafkaConfiguration configuration = new KafkaConfiguration();

    @Test
    void kafkaConsumerFactoryUsesEarliestOffsetResetByDefault() {
        var kafkaProperties = new KafkaProperties();

        var consumerFactory = configuration.kafkaConsumerFactory(
                kafkaProperties,
                kafkaConnectionDetails(),
                emptyProvider(DefaultKafkaConsumerFactoryCustomizer.class),
                emptyKafkaTransactionManagerProvider(),
                dynamicDeserializer(kafkaProperties),
                "test-app");

        assertEquals("earliest", consumerFactory.getConfigurationProperties().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    @Test
    void kafkaConsumerFactoryKeepsConfiguredOffsetReset() {
        var kafkaProperties = new KafkaProperties();
        kafkaProperties.getConsumer().getProperties().put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        var consumerFactory = configuration.kafkaConsumerFactory(
                kafkaProperties,
                kafkaConnectionDetails(),
                emptyProvider(DefaultKafkaConsumerFactoryCustomizer.class),
                emptyKafkaTransactionManagerProvider(),
                dynamicDeserializer(kafkaProperties),
                "test-app");

        assertEquals("latest", consumerFactory.getConfigurationProperties().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    @Test
    void genericKafkaProducerBeanIsCreated() {
        var producer = configuration.genericKafkaProducer(null, new EventRegistry());

        assertNotNull(producer);
    }

    @Test
    void recovererSkipsDeadLetterPublishingForUnknownEventType() {
        var dltRecoverer = mock(org.springframework.kafka.listener.DeadLetterPublishingRecoverer.class);
        var recoverer = KafkaConfiguration.unknownEventTypeIgnoringRecoverer(dltRecoverer);
        var record = new ConsumerRecord<>("topic", 0, 0L, "key", "value");

        recoverer.accept(record, new UnknownEventTypeException("unknown.Type"));

        verify(dltRecoverer, never()).accept(any(), any());
    }

    @Test
    void recovererDelegatesToDeadLetterPublishingForOtherExceptions() {
        var dltRecoverer = mock(org.springframework.kafka.listener.DeadLetterPublishingRecoverer.class);
        var recoverer = KafkaConfiguration.unknownEventTypeIgnoringRecoverer(dltRecoverer);
        var record = new ConsumerRecord<>("topic", 0, 0L, "key", "value");
        var failure = new IllegalArgumentException("boom");

        recoverer.accept(record, failure);

        verify(dltRecoverer).accept(eq(record), eq(failure));
    }

    private DynamicDeserializer dynamicDeserializer(KafkaProperties kafkaProperties) {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("test-topic", Event.Serialization.JSON);
        return new DynamicDeserializer(
                kafkaProperties,
                new DefaultConversionService(),
                eventRegistry);
    }

    private KafkaConnectionDetails kafkaConnectionDetails() {
        return () -> List.of("localhost:9092");
    }

    private <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        return new StaticListableBeanFactory().getBeanProvider(type);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<KafkaTransactionManager<?, ?>> emptyKafkaTransactionManagerProvider() {
        return (ObjectProvider<KafkaTransactionManager<?, ?>>) (ObjectProvider<?>)
                new StaticListableBeanFactory().getBeanProvider(KafkaTransactionManager.class);
    }
}


