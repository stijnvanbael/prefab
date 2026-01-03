package be.appify.prefab.core.pubsub;

import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Pub/Sub connection details.
 */
@Configuration
@ComponentScan(basePackageClasses = PubSubUtil.class)
@ConditionalOnClass(PubSubAdmin.class)
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
