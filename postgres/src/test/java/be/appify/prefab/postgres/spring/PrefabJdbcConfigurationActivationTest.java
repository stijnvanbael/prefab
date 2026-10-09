package be.appify.prefab.postgres.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PrefabJdbcConfigurationActivationTest {

    @Test
    void configuration_withoutPrefabCore_isInactive() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PrefabJdbcConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(PrefabJdbcConfiguration.class));
    }
}