package be.appify.prefab.test.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PubSubEmulatorContainer;

import javax.annotation.PreDestroy;

/**
 * Autoconfiguration for Pub/Sub tests.
 */
@Configuration
@ConditionalOnClass(PubSubAdmin.class)
@ComponentScan(basePackageClasses = PubSubTestLifecycle.class)
public class PubSubTestAutoConfiguration {
    private ManagedChannel channel;

    static final PubSubEmulatorContainer pubSubEmulatorContainer = new PubSubEmulatorContainer(
            "gcr.io/google.com/cloudsdktool/cloud-sdk:529.0.0-emulators")
            .withReuse(true)
            .withExposedPorts(8085, 8086);

    static {
        if (!pubSubEmulatorContainer.isRunning()) {
            pubSubEmulatorContainer.start();
        }
    }

    /** Constructs a new PubSubTestAutoConfiguration. */
    public PubSubTestAutoConfiguration() {
    }

    @Bean
    DynamicPropertyRegistrar pubSubPropertiesRegistrar() {
        return registry -> registry.add("spring.cloud.gcp.pubsub.emulator-host",
                pubSubEmulatorContainer::getEmulatorEndpoint);
    }

    @Bean
    CredentialsProvider credentialsProvider() {
        return NoCredentialsProvider.create();
    }

    @Bean(name = { "subscriberTransportChannelProvider", "publisherTransportChannelProvider" })
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
