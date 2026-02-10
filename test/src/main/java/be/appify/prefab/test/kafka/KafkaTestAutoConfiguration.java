package be.appify.prefab.test.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.util.SerializationRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;

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
    Network kafkaNetwork() {
        return Network.newNetwork();
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer(Network kafkaNetwork) {
        var kafkaContainer = new KafkaContainer("apache/kafka-native:4.1.1")
                .withNetwork(kafkaNetwork)
                .withReuse(true)
                .withExposedPorts(9092, 9093, 9095)
                .withListener("kafka:9095");
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }
        return kafkaContainer;
    }

    @Bean
    GenericContainer<?> kafkaSchemaRegistryContainer(KafkaContainer kafkaContainer, Network kafkaNetwork) {
        var schemaRegistryContainer = new GenericContainer<>("confluentinc/cp-schema-registry:8.0.3")
                .withReuse(true)
                .withExposedPorts(8081)
                .withNetwork(kafkaNetwork)
                .dependsOn(kafkaContainer)
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9095")
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
        if (!schemaRegistryContainer.isRunning()) {
            schemaRegistryContainer.start();
        }
        return schemaRegistryContainer;
    }

    @Bean
    @ConditionalOnBean(name = "kafkaSchemaRegistryContainer")
    DynamicPropertyRegistrar kafkaSchemaRegistryPropertiesRegistrar(GenericContainer<?> kafkaSchemaRegistryContainer) {
        return registry -> registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> "http://%s:%d".formatted(
                kafkaSchemaRegistryContainer.getHost(), kafkaSchemaRegistryContainer.getMappedPort(8081)));
    }

    @Bean
    @ConditionalOnMissingBean(name = "testConsumerFactory")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    DefaultKafkaConsumerFactory<String, Object> testConsumerFactory(
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers,
            KafkaProperties kafkaProperties,
            ConversionService conversionService,
            SerializationRegistry serializationRegistry,
            JacksonJsonTypeResolver jsonTypeResolver
    ) {
        var consumerProperties = (properties != null ? properties : kafkaProperties).buildConsumerProperties();
        consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                connectionDetails.getConsumer().getBootstrapServers());
        var factory = new DefaultKafkaConsumerFactory<String, Object>(consumerProperties);
        try (var dynamicDeserializer = new DynamicDeserializer(
                kafkaProperties,
                conversionService,
                serializationRegistry,
                jsonTypeResolver
        )) {
            factory.setValueDeserializer(dynamicDeserializer);
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
