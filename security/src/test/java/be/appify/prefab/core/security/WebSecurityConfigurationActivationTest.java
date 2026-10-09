package be.appify.prefab.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigurationActivationTest {

    @Test
    void configuration_isAutoConfigurationGatedOnPrefabCore() {
        var type = WebSecurityConfiguration.class;

        assertThat(type.isAnnotationPresent(AutoConfiguration.class)).isTrue();
        assertThat(type.getAnnotation(ConditionalOnBean.class).type())
                .containsExactly("be.appify.prefab.core.spring.PrefabCoreConfiguration");
    }
}