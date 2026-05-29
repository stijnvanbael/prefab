package be.appify.prefab.test.kafka;

import be.appify.prefab.test.TestContainerNameResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Optional;

@TestConfiguration(proxyBeanMethods = false)
@AutoConfiguration(before = KafkaTestAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaTestcontainerAutoConfiguration {
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
    @ConditionalOnMissingBean(name = "kafkaSchemaRegistryContainer")
    @ConditionalOnProperty(name = "prefab.test.schema-registry.enabled", havingValue = "true")
    GenericContainer<?> kafkaSchemaRegistryContainer(
            GenericContainer<?> kafkaContainer, Network kafkaNetwork, PropertyResolver propertyResolver) {
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
            registry.add("spring.kafka.streams.properties.schema.registry.url", () -> schemaRegistryUrl);
        };
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
                    .map(com.github.dockerjava.api.model.Network::getId)
                    .findFirst();
        }

        @Override
        public void close() {
        }
    }
}
