package be.appify.prefab.test.sns;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import be.appify.prefab.test.TestContainerNameResolver;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Autoconfiguration for SNS/SQS tests using LocalStack.
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnClass(SnsTemplate.class)
@ComponentScan(basePackageClasses = SnsTestLifecycle.class)
public class SnsTestAutoConfiguration {

    /**
     * Constructs a new SnsTestAutoConfiguration.
     */
    public SnsTestAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(name = "localStackContainer")
    LocalStackContainer localStackContainer(PropertyResolver propertyResolver) {
        var containerName = TestContainerNameResolver.resolveContainerName(
                propertyResolver, "localstack", "prefab.test.localstack.container-name");
        TestContainerNameResolver.removeConflictingContainer(containerName);
        var container = new LocalStackContainer(
                DockerImageName.parse("localstack/localstack:3"))
                .withServices(SNS, SQS)
                .withReuse(true)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
        if (!container.isRunning()) {
            container.start();
        }
        return container;
    }

    @Bean
    DynamicPropertyRegistrar snsPropertiesRegistrar(LocalStackContainer localStackContainer) {
        return registry -> {
            registry.add("spring.cloud.aws.region.static", localStackContainer::getRegion);
            registry.add("spring.cloud.aws.credentials.access-key", localStackContainer::getAccessKey);
            registry.add("spring.cloud.aws.credentials.secret-key", localStackContainer::getSecretKey);
            registry.add("spring.cloud.aws.sns.endpoint",
                    () -> localStackContainer.getEndpointOverride(SNS).toString());
            registry.add("spring.cloud.aws.sqs.endpoint",
                    () -> localStackContainer.getEndpointOverride(SQS).toString());
        };
    }
}
