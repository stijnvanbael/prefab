package be.appify.prefab.test.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Autoconfiguration for Kafka test support.
 */
@Configuration
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
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        var kafkaContainer = new KafkaContainer("apache/kafka-native:3.9.1")
                .withReuse(true)
                .withExposedPorts(9092, 9093);
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }
        return kafkaContainer;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonTestConsumerFactory")
    @ConditionalOnClass(JacksonJsonDeserializer.class)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    DefaultKafkaConsumerFactory<String, Object> jsonTestConsumerFactory(
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers,
            JsonMapper jsonMapper,
            TestJsonTypeResolver jsonTypeResolver
    ) {
        var consumerProperties = this.properties.buildConsumerProperties();
        consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                connectionDetails.getConsumer().getBootstrapServers());
        var factory = new DefaultKafkaConsumerFactory<String, Object>(consumerProperties);
        try (var jsonDeserializer = new JacksonJsonDeserializer<>(jsonMapper)) {
            jsonDeserializer.typeResolver(jsonTypeResolver::resolveType);
            factory.setValueDeserializer(jsonDeserializer);
        }
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
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
