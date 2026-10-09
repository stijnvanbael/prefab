package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for Pub/Sub connection details.
 */
@AutoConfiguration(after = PrefabRegistryConfiguration.class)
@Import({PubSubUtil.class, PubSubSerializer.class, PubSubDeserializer.class, GenericPubSubPublisher.class})
@ConditionalOnClass(PubSubAdmin.class)
@ConditionalOnBean(PrefabCoreConfiguration.class)
public class PubSubConfiguration {

    /** Constructs a new PubSubConfiguration. */
    public PubSubConfiguration() {
    }

    /**
     * Creates a PubSubConnectionDetails bean if none is already defined.
     *
     * @param properties the GCP Pub/Sub properties
     * @return a PubSubConnectionDetails instance
     */
    @Bean
    @ConditionalOnMissingBean
    public PubSubConnectionDetails pubSubConnectionDetails(GcpPubSubProperties properties) {
        return new PropertiesPubSubConnectionDetails(properties);
    }
}
