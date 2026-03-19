package be.appify.prefab.core.sns;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for SNS/SQS support.
 */
@Configuration
@ComponentScan(basePackageClasses = SqsUtil.class)
@ConditionalOnClass(SnsTemplate.class)
public class SnsConfiguration {

    /** Constructs a new SnsConfiguration. */
    public SnsConfiguration() {
    }
}
