package be.appify.prefab.test.inmemory;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAutoConfigurationFilterTest {

    private static final String REGISTRY = "be.appify.prefab.core.kafka.PrefabRegistryConfiguration";

    static Stream<String> excludedClassNames() {
        return InMemoryAutoConfigurationFilter.EXCLUDED.stream();
    }

    @ParameterizedTest
    @MethodSource("excludedClassNames")
    void excludedClassExistsOnClasspath(String className) {
        assertThat(className).satisfies(name -> assertThat(Class.forName(name, false, getClass().getClassLoader()))
                .as("%s must exist so a rename fails the build", name).isNotNull());
    }

    @Test
    void inMemoryModeDropsTransportConfigurationsAndKeepsOthers() {
        var filter = filterFor("false");
        var candidates = new String[]{
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", REGISTRY,
                "be.appify.prefab.test.kafka.KafkaTestcontainerAutoConfiguration"};

        var result = filter.match(candidates, null);

        assertThat(result).containsExactly(false, true, false);
    }

    @Test
    void candidatesRejectedByAnEarlierFilterAreIgnored() {
        var filter = filterFor("false");
        var candidates = new String[]{null, REGISTRY};

        var result = filter.match(candidates, null);

        assertThat(result).containsExactly(true, true);
    }

    @Test
    void inMemoryIsTheDefaultWhenThePropertyIsAbsent() {
        var filter = filterFor(null);

        var result = filter.match(new String[]{"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"}, null);

        assertThat(result).containsExactly(false);
    }

    @Test
    void containersModeKeepsEverything() {
        var filter = filterFor("true");
        var candidates = InMemoryAutoConfigurationFilter.EXCLUDED.toArray(String[]::new);

        var result = filter.match(candidates, null);

        assertThat(result).doesNotContain(false);
    }

    private static InMemoryAutoConfigurationFilter filterFor(String containers) {
        var environment = new MockEnvironment();
        if (containers != null) {
            environment.setProperty(TestInfrastructureMode.PROPERTY, containers);
        }
        var filter = new InMemoryAutoConfigurationFilter();
        filter.setEnvironment(environment);
        return filter;
    }
}
