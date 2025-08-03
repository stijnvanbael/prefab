package be.appify.prefab.processor.pubsub;

import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
