package be.appify.prefab.test.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import be.appify.prefab.core.util.SerializationRegistry;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertyResolver;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import be.appify.prefab.test.TestContainerNameResolver;

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
    @ConditionalOnMissingBean(name = "kafkaNetwork")
    Network kafkaNetwork(PropertyResolver propertyResolver) {
        return new ReusableNetwork(kafkaNetworkName(propertyResolver));
    }

    static String kafkaNetworkName(PropertyResolver propertyResolver) {
        return TestContainerNameResolver.resolveContainerName(
                propertyResolver, "kafka", "prefab.test.kafka.network-name");
    }

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean(name = "kafkaContainer")
    KafkaContainer kafkaContainer(Network kafkaNetwork, PropertyResolver propertyResolver) {
        var containerName = TestContainerNameResolver.resolveContainerName(
                propertyResolver, "kafka", "prefab.test.kafka.container-name");
        TestContainerNameResolver.removeConflictingContainer(containerName);
        return new KafkaContainer("apache/kafka:4.0.2")
                .withNetwork(kafkaNetwork)
                .withNetworkAliases("kafka")
                .withListener("0.0.0.0:9095", () -> "kafka:9095")
                .withReuse(true)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
    }

    @Bean
    @ConditionalOnProperty(name = "prefab.test.schema-registry.enabled", havingValue = "true")
    GenericContainer<?> kafkaSchemaRegistryContainer(
            KafkaContainer kafkaContainer, Network kafkaNetwork, PropertyResolver propertyResolver) {
        var containerName = TestContainerNameResolver.resolveContainerName(
                propertyResolver, "schema-registry", "prefab.test.schema-registry.container-name");
        TestContainerNameResolver.removeConflictingContainer(containerName);
        return new GenericContainer<>("confluentinc/cp-schema-registry:8.1.2")
                .withExposedPorts(8081)
                .withNetwork(kafkaNetwork)
                .dependsOn(kafkaContainer)
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9095")
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                .withReuse(true)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
    }

    @Bean
    @ConditionalOnBean(name = "kafkaSchemaRegistryContainer")
    DynamicPropertyRegistrar kafkaSchemaRegistryPropertiesRegistrar(GenericContainer<?> kafkaSchemaRegistryContainer) {
        return registry -> {
            var schemaRegistryUrl = "http://%s:%d".formatted(
                    kafkaSchemaRegistryContainer.getHost(), kafkaSchemaRegistryContainer.getMappedPort(8081));
            registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> schemaRegistryUrl);
            registry.add("spring.kafka.producer.properties.schema.registry.url", () -> schemaRegistryUrl);
        };
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
            KafkaJsonTypeResolver jsonTypeResolver
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

    private static final class ReusableNetwork implements Network {

        private final String name;
        private String id;

        private ReusableNetwork(String name) {
            this.name = name;
        }

        @Override
        public synchronized String getId() {
            if (this.id == null) {
                this.id = findOrCreateNetworkId();
            }
            return this.id;
        }

        private String findOrCreateNetworkId() {
            var dockerClient = DockerClientFactory.instance().client();
            var existingNetworkId = findExistingNetworkId(dockerClient);
            if (existingNetworkId.isPresent()) {
                return existingNetworkId.get();
            }

            try {
                return dockerClient.createNetworkCmd()
                        .withName(this.name)
                        .withCheckDuplicate(true)
                        .exec()
                        .getId();
            } catch (RuntimeException exception) {
                return findExistingNetworkId(dockerClient)
                        .orElseThrow(() -> new IllegalStateException(
                                "Unable to create or locate Kafka test network '" + this.name + "'", exception));
            }
        }

        private Optional<String> findExistingNetworkId(com.github.dockerjava.api.DockerClient dockerClient) {
            return dockerClient.listNetworksCmd()
                    .withNameFilter(this.name)
                    .exec()
                    .stream()
                    .map(network -> network.getId())
                    .findFirst();
        }

        @Override
        public void close() {
        }
    }
}
