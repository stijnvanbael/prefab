package be.appify.prefab.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class TestContainerNameResolverTest {

    private static final String NAME_PROPERTY = "prefab.test.pubsub.container-name";

    @Test
    @DisplayName("resolveUniqueContainerName_givenSameInput_returnsDifferentNames")
    void resolveUniqueContainerName_givenSameInput_returnsDifferentNames() {
        var environment = new MockEnvironment().withProperty("spring.application.name", "my-service");

        var first = TestContainerNameResolver.resolveUniqueContainerName(environment, "pubsub", NAME_PROPERTY);
        var second = TestContainerNameResolver.resolveUniqueContainerName(environment, "pubsub", NAME_PROPERTY);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("resolveUniqueContainerName_givenApplicationName_extendsDefaultName")
    void resolveUniqueContainerName_givenApplicationName_extendsDefaultName() {
        var environment = new MockEnvironment().withProperty("spring.application.name", "my-service");

        var name = TestContainerNameResolver.resolveUniqueContainerName(environment, "pubsub", NAME_PROPERTY);

        assertThat(name).matches("pubsub_my_service_[0-9a-f]{8}");
    }

    @Test
    @DisplayName("resolveUniqueContainerName_givenCustomName_extendsCustomName")
    void resolveUniqueContainerName_givenCustomName_extendsCustomName() {
        var environment = new MockEnvironment().withProperty(NAME_PROPERTY, "my_emulator");

        var name = TestContainerNameResolver.resolveUniqueContainerName(environment, "pubsub", NAME_PROPERTY);

        assertThat(name).matches("my_emulator_[0-9a-f]{8}");
    }
}
