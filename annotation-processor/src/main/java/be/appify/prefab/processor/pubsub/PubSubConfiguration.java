package be.appify.prefab.processor.pubsub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = PubSubUtil.class)
public class PubSubConfiguration {
    @ConditionalOnMissingBean
    public PubSubConnectionDetails pubSubConnectionDetails(GcpPubSubProperties properties) {
        return new PropertiesPubSubConnectionDetails(properties);
    }
}
