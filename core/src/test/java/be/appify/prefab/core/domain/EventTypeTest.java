package be.appify.prefab.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTypeTest {
    record Changed<T>(String id, T payload) {}

    @Test
    void capturesAndComparesConcreteArguments() {
        var first = new EventType<Changed<String>>() {};
        var second = new EventType<Changed<String>>() {};
        var different = new EventType<Changed<Integer>>() {};

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo(different);
        assertThat(first.rawType()).isEqualTo(Changed.class);
        assertThat(first.id()).contains("Changed<java.lang.String>");
    }

    @Test
    void rejectsRawAndWildcardArguments() {
        assertThatThrownBy(() -> EventType.of(Changed.class))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-raw");
        assertThatThrownBy(() -> new EventType<Changed<?>>() {})
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("concrete");
    }

    @Test
    void classAndCapturedNonGenericTokensHaveTheSameIdentity() {
        assertThat(EventType.of(String.class)).isEqualTo(new EventType<String>() {});
    }

    @Test
    void rejectsUnresolvedTypeVariables() {
        assertThatThrownBy(EventTypeTest::<String>unresolvedToken)
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("concrete");
    }

    private static <T> EventType<Changed<T>> unresolvedToken() {
        return new EventType<Changed<T>>() {};
    }
}
