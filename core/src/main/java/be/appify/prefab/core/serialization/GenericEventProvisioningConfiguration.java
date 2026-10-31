package be.appify.prefab.core.serialization;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.sns.SqsUtil;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/** Provisions every concrete event topic after registration, including bindings without handlers or publications. */
@AutoConfiguration(after = PrefabRegistryConfiguration.class,
        afterName = {"org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration",
                "be.appify.prefab.core.kafka.KafkaConfiguration",
                "be.appify.prefab.core.pubsub.PubSubConfiguration",
                "be.appify.prefab.core.sns.SnsConfiguration"})
public class GenericEventProvisioningConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GenericEventProvisioningConfiguration.class);

    /** Creates the generic-topic provisioning configuration. */
    public GenericEventProvisioningConfiguration() {}

    private static SmartInitializingSingleton provision(EventRegistry registry, Event.Platform platform, Consumer<String> create) {
        return () -> {
            var topics = registry.concreteTopicsFor(platform).stream().sorted().toList();
            if (!topics.isEmpty()) {
                log.info("Provisioning {} concrete event topics on {}", topics.size(), platform);
                topics.forEach(create);
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaAdmin.class)
    static class Kafka {
        @Bean
        @ConditionalOnBean({EventRegistry.class, KafkaAdmin.class})
        SmartInitializingSingleton genericKafkaTopics(EventRegistry registry, KafkaAdmin admin) {
            return provision(registry, Event.Platform.KAFKA, topic ->
                    admin.createOrModifyTopics(new NewTopic(topic, Optional.empty(), Optional.empty())));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(PubSubAdmin.class)
    static class PubSub {
        @Bean
        @ConditionalOnBean({EventRegistry.class, PubSubUtil.class})
        SmartInitializingSingleton genericPubSubTopics(EventRegistry registry, PubSubUtil util) {
            return provision(registry, Event.Platform.PUB_SUB, util::ensureTopicExists);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SnsTemplate.class)
    static class Sns {
        @Bean
        @ConditionalOnBean({EventRegistry.class, SqsUtil.class})
        SmartInitializingSingleton genericSnsTopics(EventRegistry registry, SqsUtil util) {
            return provision(registry, Event.Platform.SNS_SQS, util::ensureTopicExists);
        }
    }
}
