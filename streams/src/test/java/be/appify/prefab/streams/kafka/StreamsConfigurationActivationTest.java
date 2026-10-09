package be.appify.prefab.streams.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class StreamsConfigurationActivationTest {

    @Test
    void configuration_withoutPrefabCore_isInactive() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(StreamsConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(StreamsConfiguration.class));
    }
}