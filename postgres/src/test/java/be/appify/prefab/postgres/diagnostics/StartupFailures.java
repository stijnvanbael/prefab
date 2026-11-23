package be.appify.prefab.postgres.diagnostics;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Starts an application like a real one, so that Spring Boot reports the failure through its failure analyzers. */
final class StartupFailures {
    private StartupFailures() {
    }

    static void startExpectingFailure(Class<?> configuration, String... properties) {
        assertThatThrownBy(() -> new SpringApplicationBuilder(configuration)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run())
                .isNotNull();
    }
}
