package be.appify.prefab.test.pubsub;

import be.appify.prefab.processor.pubsub.PubSubConnectionDetails;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;

import javax.annotation.PreDestroy;

@Configuration
@ComponentScan(basePackageClasses = PubSubTestLifecycle.class)
public class PubSubTestAutoConfiguration {
    private ManagedChannel channel;

    @Bean
    DynamicPropertyRegistrar pubSubPropertiesRegistrar(PubSubConnectionDetails connectionDetails) {
        return registry -> registry.add("spring.cloud.gcp.pubsub.emulator-host", connectionDetails::getEmulatorHost);
    }

    @Bean
    CredentialsProvider credentialsProvider() {
        return NoCredentialsProvider.create();
    }

    @Bean(name = {"subscriberTransportChannelProvider", "publisherTransportChannelProvider"})
    TransportChannelProvider transportChannelProvider(GcpPubSubProperties gcpPubSubProperties) {
        this.channel = ManagedChannelBuilder
                .forTarget("dns:///" + gcpPubSubProperties.getEmulatorHost())
                .usePlaintext()
                .build();
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(this.channel));
    }

    @PreDestroy
    public void closeManagedChannel() {
        if (this.channel != null) {
            this.channel.shutdown();
        }
    }
}
