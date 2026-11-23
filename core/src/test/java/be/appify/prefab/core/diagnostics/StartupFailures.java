package be.appify.prefab.core.diagnostics;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Starts an application like a real one, so that Spring Boot reports the failure through its failure analyzers. */
final class StartupFailures {
    private StartupFailures() {
    }

    /**
     * Starts an application that is expected to fail and prints the failure report to the console.
     *
     * @param configuration the configuration class of the application
     * @param properties    the properties as {@code key=value}
     */
    static void startExpectingFailure(Class<?> configuration, String... properties) {
        assertThatThrownBy(() -> new SpringApplicationBuilder(configuration)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run())
                .isNotNull();
    }
}
