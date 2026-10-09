package be.appify.prefab.core.sns;

import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for SNS/SQS support.
 */
@AutoConfiguration(after = PrefabRegistryConfiguration.class)
@Import({SqsUtil.class, SnsSerializer.class, SqsDeserializer.class, GenericSnsPublisher.class})
@ConditionalOnClass(SnsTemplate.class)
@ConditionalOnBean(PrefabCoreConfiguration.class)
public class SnsConfiguration {

    /** Constructs a new SnsConfiguration. */
    public SnsConfiguration() {
    }
}
