package be.appify.prefab.test.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import be.appify.prefab.core.util.SerializationRegistry;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaTestAutoConfigurationTest {

    @Test
    void testConsumerFactoryUsesLatestOffsetResetByDefault() {
        var properties = new KafkaProperties();
        var configuration = new KafkaTestAutoConfiguration(properties);

        var consumerFactory = configuration.testConsumerFactory(
                kafkaConnectionDetails(),
                emptyProvider(DefaultKafkaConsumerFactoryCustomizer.class),
                properties,
                new DefaultConversionService(),
                new SerializationRegistry(),
                new KafkaJsonTypeResolver());

        assertEquals("latest", consumerFactory.getConfigurationProperties().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    @Test
    void testConsumerFactoryKeepsConfiguredOffsetReset() {
        var properties = new KafkaProperties();
        properties.getConsumer().getProperties().put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var configuration = new KafkaTestAutoConfiguration(properties);

        var consumerFactory = configuration.testConsumerFactory(
                kafkaConnectionDetails(),
                emptyProvider(DefaultKafkaConsumerFactoryCustomizer.class),
                properties,
                new DefaultConversionService(),
                new SerializationRegistry(),
                new KafkaJsonTypeResolver());

        assertEquals("earliest", consumerFactory.getConfigurationProperties().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    private KafkaConnectionDetails kafkaConnectionDetails() {
        return () -> List.of("localhost:9092");
    }

    private <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        return new StaticListableBeanFactory().getBeanProvider(type);
    }
}

