package be.appify.prefab.core.diagnostics;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.core.kafka.KafkaConfiguration;
import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.pubsub.PubSubConfiguration;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.sns.SnsConfiguration;
import be.appify.prefab.core.spring.EnablePrefab;
import be.appify.prefab.core.tenant.TenantContextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import static be.appify.prefab.core.diagnostics.StartupFailures.startExpectingFailure;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void prefabNotEnabled(CapturedOutput output) {
        startExpectingFailure(PrefabNotEnabled.class);

        assertThat(output)
                .contains("Prefab is not enabled")
                .contains(DomainEventPublisher.class.getName())
                .contains("Add @EnablePrefab to your @SpringBootApplication class");
    }

    @Test
    void missingApplicationName(CapturedOutput output) {
        startExpectingFailure(KafkaWithoutApplicationName.class);

        assertThat(output)
                .contains("The property spring.application.name is not set")
                .contains("Set spring.application.name in application.yml");
    }

    @Test
    void missingSchemaRegistryUrl(CapturedOutput output) {
        startExpectingFailure(KafkaWithAvroWithoutRegistry.class, "spring.application.name=test-app");

        assertThat(output)
                .contains("Missing Kafka property 'schema.registry.url' for AVRO event serialization")
                .contains("orders")
                .contains("Configure a real schema registry URL");
    }

    @Test
    void unknownNonRetryableException(CapturedOutput output) {
        startExpectingFailure(KafkaWithoutApplicationName.class,
                "spring.application.name=test-app",
                "prefab.dlt.non-retryable-exceptions=com.acme.DoesNotExist");

        assertThat(output)
                .contains("The class [com.acme.DoesNotExist] could not be loaded")
                .contains("prefab.dlt.non-retryable-exceptions")
                .contains("fully qualified name");
    }

    @Test
    void missingSnsClient(CapturedOutput output) {
        startExpectingFailure(SnsWithoutClient.class, "spring.application.name=test-app");

        assertThat(output)
                .contains("Prefab needs an SNS client, but Spring Cloud AWS did not create it")
                .contains("spring-cloud-aws-starter-sns");
    }

    @Test
    void missingGcpProjectId(CapturedOutput output) {
        startExpectingFailure(PubSubWithoutProjectId.class, "spring.application.name=test-app");

        assertThat(output)
                .contains("The property spring.cloud.gcp.project-id is not set")
                .contains("Set spring.cloud.gcp.project-id");
    }

    @Test
    void missingPubSubBeans(CapturedOutput output) {
        startExpectingFailure(PubSubWithoutProject.class,
                "spring.application.name=test-app",
                "spring.cloud.gcp.project-id=test-project");

        assertThat(output)
                .contains("Prefab needs a Google Cloud Pub/Sub bean, but Spring Cloud GCP did not create it")
                .contains("spring.cloud.gcp.pubsub.emulator-host");
    }

    @Test
    void severalTenantContextProviders(CapturedOutput output) {
        startExpectingFailure(TwoTenantProviders.class);

        assertThat(output)
                .contains("Multi-tenancy needs exactly one TenantContextProvider")
                .contains("firstProvider")
                .contains("secondProvider")
                .contains("mark the one to use as @Primary");
    }

    @Test
    void originalExceptionStaysAvailableWithDebugLogging(CapturedOutput output) {
        startExpectingFailure(PrefabNotEnabled.class,
                "logging.level.org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter=DEBUG");

        assertThat(output)
                .contains("Application failed to start due to an exception")
                .contains("NoSuchBeanDefinitionException");
    }

    @Configuration(proxyBeanMethods = false)
    static class PrefabNotEnabled {
        @Bean
        Object needsPublisher(DomainEventPublisher publisher) {
            return publisher;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import(Infrastructure.class)
    @ImportAutoConfiguration({KafkaAutoConfiguration.class, PrefabRegistryConfiguration.class,
            KafkaConfiguration.class})
    static class KafkaWithoutApplicationName {
        @Bean
        EventRegistryCustomizer events() {
            return registry -> registry.register("orders", Event.Serialization.JSON);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import(Infrastructure.class)
    @ImportAutoConfiguration({KafkaAutoConfiguration.class, PrefabRegistryConfiguration.class,
            KafkaConfiguration.class})
    static class KafkaWithAvroWithoutRegistry {
        @Bean
        EventRegistryCustomizer events() {
            return registry -> registry.register("orders", Event.Serialization.AVRO);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import(Infrastructure.class)
    @ImportAutoConfiguration({PrefabRegistryConfiguration.class, SnsConfiguration.class})
    static class SnsWithoutClient {
        @Bean
        EventRegistryCustomizer events() {
            return registry -> registry.register("orders", Event.Serialization.JSON);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import(Infrastructure.class)
    @ImportAutoConfiguration({PrefabRegistryConfiguration.class, PubSubConfiguration.class})
    static class PubSubWithoutProject {
        @Bean
        EventRegistryCustomizer events() {
            return registry -> registry.register("orders", Event.Serialization.JSON);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import({Infrastructure.class, PubSubUtil.class})
    static class PubSubWithoutProjectId {
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import(Infrastructure.class)
    static class TwoTenantProviders {
        @Bean
        TenantContextProvider firstProvider() {
            return () -> "a";
        }

        @Bean
        TenantContextProvider secondProvider() {
            return () -> "b";
        }

        @Bean
        Object consumer(TenantContextProvider provider) {
            return provider;
        }
    }

    /** The Spring Boot infrastructure that Prefab expects from a web application. */
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({JacksonAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
    static class Infrastructure {
        @Bean
        ConversionService conversionService() {
            return new DefaultConversionService();
        }
    }
}
