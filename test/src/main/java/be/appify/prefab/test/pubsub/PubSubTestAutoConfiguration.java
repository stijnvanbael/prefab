package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import be.appify.prefab.core.pubsub.PubSubConfiguration;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import be.appify.prefab.test.TestContainerNameResolver;

/**
 * Autoconfiguration for Pub/Sub tests.
 */
@AutoConfiguration(after = PubSubConfiguration.class)
@ConditionalOnClass(PubSubAdmin.class)
@ConditionalOnBean(PrefabCoreConfiguration.class)
@Import(PubSubTestLifecycle.class)
public class PubSubTestAutoConfiguration {
    static final String PUBLISH_TIMEOUT_PROPERTY = "spring.cloud.gcp.pubsub.publisher.retry.total-timeout-seconds";
    static final String PUBLISH_RPC_TIMEOUT_PROPERTY = "spring.cloud.gcp.pubsub.publisher.retry.max-rpc-timeout-seconds";
    static final String PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY =
            "spring.cloud.gcp.pubsub.publisher.retry.initial-rpc-timeout-seconds";
    static final long PUBLISH_TIMEOUT_SECONDS = 20;
    static final long PUBLISH_INITIAL_RPC_TIMEOUT_SECONDS = 5;

    private ManagedChannel channel;

    /**
     * Constructs a new PubSubTestAutoConfiguration.
     */
    public PubSubTestAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(name = "pubSubEmulatorContainer")
    PubSubEmulatorContainer pubSubEmulatorContainer(PropertyResolver propertyResolver) {
        // Every context gets its own emulator: topics and subscriptions are not shared between cached contexts, and
        // starting one must never remove the emulator of a context that Spring's test context cache still holds.
        var containerName = TestContainerNameResolver.resolveUniqueContainerName(
                propertyResolver, "pubsub", "prefab.test.pubsub.container-name");
        var container = new PubSubEmulatorContainer(
                "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators")
                .withReuse(false)
                .withExposedPorts(8085, 8086)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
        if (!container.isRunning()) {
            container.start();
        }
        return container;
    }

    @Bean
    DynamicPropertyRegistrar pubSubPropertiesRegistrar(
            PubSubEmulatorContainer pubSubEmulatorContainer,
            PropertyResolver propertyResolver) {
        return registry -> {
            registry.add("spring.cloud.gcp.pubsub.emulator-host", pubSubEmulatorContainer::getEmulatorEndpoint);
            registerPublishTimeouts(registry, propertyResolver);
        };
    }

    // Without a bound the publisher retries a vanished emulator for minutes per publish, which looks like a hang.
    // The timeouts must stay consistent with each other, so they are only defaulted when none of them is configured.
    private static void registerPublishTimeouts(DynamicPropertyRegistry registry, PropertyResolver propertyResolver) {
        var configured = Stream.of(
                        PUBLISH_TIMEOUT_PROPERTY, PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY, PUBLISH_RPC_TIMEOUT_PROPERTY)
                .anyMatch(propertyResolver::containsProperty);
        if (!configured) {
            registry.add(PUBLISH_TIMEOUT_PROPERTY, () -> PUBLISH_TIMEOUT_SECONDS);
            registry.add(PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY, () -> PUBLISH_INITIAL_RPC_TIMEOUT_SECONDS);
            registry.add(PUBLISH_RPC_TIMEOUT_PROPERTY, () -> PUBLISH_TIMEOUT_SECONDS);
        }
    }

    @Bean
    @ConditionalOnMissingBean(CredentialsProvider.class)
    CredentialsProvider credentialsProvider() {
        return NoCredentialsProvider.create();
    }

    @Bean(name = { "subscriberTransportChannelProvider", "publisherTransportChannelProvider" })
    @ConditionalOnMissingBean(TransportChannelProvider.class)
    TransportChannelProvider transportChannelProvider(GcpPubSubProperties gcpPubSubProperties) {
        this.channel = ManagedChannelBuilder
                .forTarget("dns:///" + gcpPubSubProperties.getEmulatorHost())
                .usePlaintext()
                .build();
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(this.channel));
    }

    /**
     * Closes the managed channel when the context is destroyed.
     */
    @PreDestroy
    public void closeManagedChannel() {
        if (this.channel != null) {
            this.channel.shutdown();
        }
    }
}
