package be.appify.prefab.core.pubsub;

import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = PubSubUtil.class)
@ConditionalOnClass(PubSubAdmin.class)
public class PubSubConfiguration {
    @ConditionalOnMissingBean
    public PubSubConnectionDetails pubSubConnectionDetails(GcpPubSubProperties properties) {
        return new PropertiesPubSubConnectionDetails(properties);
    }
}
