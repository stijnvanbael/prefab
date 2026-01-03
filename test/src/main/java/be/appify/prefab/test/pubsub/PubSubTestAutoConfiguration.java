package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubConnectionDetails;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;

import javax.annotation.PreDestroy;

/**
 * Auto-configuration for Pub/Sub tests.
 */
@Configuration
@ConditionalOnClass(PubSubAdmin.class)
@ComponentScan(basePackageClasses = PubSubTestLifecycle.class)
public class PubSubTestAutoConfiguration {
    private ManagedChannel channel;

    /** Constructs a new PubSubTestAutoConfiguration. */
    public PubSubTestAutoConfiguration() {
    }

    @Bean
    @ConditionalOnBean(PubSubConnectionDetails.class)
    DynamicPropertyRegistrar pubSubPropertiesRegistrar(PubSubConnectionDetails connectionDetails) {
        return registry -> registry.add("spring.cloud.gcp.pubsub.emulator-host", connectionDetails::getEmulatorHost);
    }

    @Bean
    CredentialsProvider credentialsProvider() {
        return NoCredentialsProvider.create();
    }

    @Bean(name = { "subscriberTransportChannelProvider", "publisherTransportChannelProvider" })
    @ConditionalOnBean(PubSubConnectionDetails.class)
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
