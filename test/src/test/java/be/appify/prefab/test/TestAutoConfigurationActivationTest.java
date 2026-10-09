package be.appify.prefab.test;

import be.appify.prefab.test.kafka.KafkaTestAutoConfiguration;
import be.appify.prefab.test.kafka.KafkaTestcontainerAutoConfiguration;
import be.appify.prefab.test.mongodb.MongoDbTestAutoConfiguration;
import be.appify.prefab.test.persistence.PostgresTestAutoConfiguration;
import be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration;
import be.appify.prefab.test.security.SecurityMockMvcTestAutoConfiguration;
import be.appify.prefab.test.sns.SnsTestAutoConfiguration;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TestAutoConfigurationActivationTest {

    static Stream<Class<?>> testAutoConfigurations() {
        return Stream.of(
                PostgresTestAutoConfiguration.class,
                KafkaTestcontainerAutoConfiguration.class,
                KafkaTestAutoConfiguration.class,
                PubSubTestAutoConfiguration.class,
                SnsTestAutoConfiguration.class,
                MongoDbTestAutoConfiguration.class,
                SecurityMockMvcTestAutoConfiguration.class);
    }

    @ParameterizedTest
    @MethodSource("testAutoConfigurations")
    void testAutoConfiguration_withoutPrefabCore_isInactive(Class<?> configuration) {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(configuration))
                .run(context -> assertThat(context).doesNotHaveBean(configuration));
    }
}