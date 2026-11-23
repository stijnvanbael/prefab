package be.appify.prefab.streams;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamBackendTest {

    @Test
    @DisplayName("backends with the same name are equal regardless of case and whitespace")
    void named_givenSameNameInDifferentCase_isEqual() {
        assertThat(StreamBackend.named(" Kafka ")).isEqualTo(StreamBackend.named("kafka"));
    }

    @Test
    @DisplayName("backends with different names are not equal")
    void named_givenDifferentNames_isNotEqual() {
        assertThat(StreamBackend.named("kafka")).isNotEqualTo(StreamBackend.named("pubsub"));
    }

    @Test
    @DisplayName("toString is the normalized name")
    void toString_returnsName() {
        assertThat(StreamBackend.named("PubSub")).hasToString("pubsub");
    }

    @Test
    @DisplayName("a blank name is rejected")
    void named_givenBlankName_throws() {
        assertThatThrownBy(() -> StreamBackend.named(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("a null name is rejected")
    void named_givenNull_throws() {
        assertThatThrownBy(() -> StreamBackend.named(null)).isInstanceOf(NullPointerException.class);
    }
}
