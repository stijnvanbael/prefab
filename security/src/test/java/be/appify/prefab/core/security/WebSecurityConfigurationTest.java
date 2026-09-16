package be.appify.prefab.core.security;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigurationTest {

    private final WebSecurityConfiguration configuration = new WebSecurityConfiguration();

    @Test
    void applyCustomizersAllowsMissingCustomizers() throws Exception {
        configuration.applyCustomizers(null, emptyProvider(HttpSecurityCustomizer.class));
    }

    @Test
    void applyCustomizersInvokesRegisteredCustomizersInOrder() throws Exception {
        HttpSecurity http = null;
        var invocations = new ArrayList<String>();
        var beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("later", new OrderedCustomizer(2, "later", invocations, http));
        beanFactory.addBean("first", new OrderedCustomizer(1, "first", invocations, http));

        configuration.applyCustomizers(http, beanFactory.getBeanProvider(HttpSecurityCustomizer.class));

        assertThat(invocations).containsExactly("first", "later");
    }

    private <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        return new StaticListableBeanFactory().getBeanProvider(type);
    }

    private record OrderedCustomizer(
            int order,
            String name,
            List<String> invocations,
            HttpSecurity expectedHttp
    ) implements HttpSecurityCustomizer, Ordered {

        @Override
        public void customize(HttpSecurity http) {
            assertThat(http).isSameAs(expectedHttp);
            invocations.add(name);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
