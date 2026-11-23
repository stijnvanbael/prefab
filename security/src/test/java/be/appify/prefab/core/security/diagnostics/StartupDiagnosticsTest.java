package be.appify.prefab.core.security.diagnostics;

import be.appify.prefab.core.security.WebSecurityConfiguration;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void missingJwtDecoder(CapturedOutput output) {
        assertThatThrownBy(() -> new SpringApplicationBuilder(SecuredApplication.class)
                .web(WebApplicationType.SERVLET)
                .contextFactory(type -> servletContext())
                .run())
                .isNotNull();

        assertThat(output)
                .contains("prefab-security validates bearer tokens as JWTs")
                .contains("spring.security.oauth2.resourceserver.jwt.issuer-uri")
                .contains("define a bean named prefabSecurityFilterChain");
    }

    private static AnnotationConfigServletWebApplicationContext servletContext() {
        var context = new AnnotationConfigServletWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getBeanFactory().registerSingleton("prefabCoreConfiguration", new PrefabCoreConfiguration());
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(WebSecurityConfiguration.class)
    static class SecuredApplication {
    }
}
