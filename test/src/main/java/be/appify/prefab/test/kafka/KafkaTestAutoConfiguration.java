package be.appify.prefab.test.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.EventRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;

import java.util.Map;

/**
 * Autoconfiguration for Kafka test support.
 */
@TestConfiguration(proxyBeanMethods = false)
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaTestAutoConfiguration {

    private final KafkaProperties properties;

    /**
     * Constructs a JsonTestConsumerFactoryAutoConfiguration with the given Kafka properties.
     *
     * @param properties
     *         the Kafka properties
     */
    public KafkaTestAutoConfiguration(KafkaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "testConsumerFactory")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    DefaultKafkaConsumerFactory<String, Object> testConsumerFactory(
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers,
            KafkaProperties kafkaProperties,
            ConversionService conversionService,
            EventRegistry eventRegistry
    ) {
        var consumerProperties = (properties != null ? properties : kafkaProperties).buildConsumerProperties();
        consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                connectionDetails.getConsumer().getBootstrapServers());
        var factory = new DefaultKafkaConsumerFactory<String, Object>(consumerProperties);
        try (var dynamicDeserializer = new DynamicDeserializer(
                kafkaProperties,
                conversionService,
                eventRegistry
        )) {
            factory.setValueDeserializer(dynamicDeserializer);
        }
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    /**
     * Overrides the application-level Kafka consumer factory's {@code auto.offset.reset} to
     * {@code latest} in tests when the property has not been explicitly configured by the user.
     *
     * <p>This applies to generated {@code @KafkaListener} consumers (e.g. those backing
     * {@code @EventHandler} methods). It does <em>not</em> affect the {@code testConsumerFactory}
     * used by {@code @TestEventConsumer}, which retains {@code earliest} so test-assertion consumers
     * reliably catch any event regardless of partition-assignment timing.
     */
    @Bean
    @ConditionalOnClass(KafkaListenerContainerFactory.class)
    DefaultKafkaConsumerFactoryCustomizer testLatestOffsetResetCustomizer(KafkaProperties kafkaProperties) {
        return factory -> {
            var explicitValue = kafkaProperties.buildConsumerProperties()
                    .get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG);
            if (explicitValue == null) {
                factory.updateConfigs(Map.of(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "testJsonTypeResolver")
    @ConditionalOnClass(JacksonJsonDeserializer.class)
    TestJsonTypeResolver testJsonTypeResolver(ObjectProvider<JacksonJsonTypeResolver> delegate) {
        return new TestJsonTypeResolver(delegate.getIfAvailable(() -> (topic, data, headers) -> {
            throw new IllegalStateException("No type resolver configured for topic: " + topic);
        }));
    }
}
