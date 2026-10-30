package be.appify.prefab.streams.tracing;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTracingTest {
    @Test
    void union_shouldDropDuplicatesAndKeepOrder() {
        assertThat(MessageTracing.union(List.of("a", "b"), List.of("b", "c"), 10)).containsExactly("a", "b", "c");
    }

    @Test
    void union_shouldEvictOldestWhenCapIsExceeded() {
        assertThat(MessageTracing.union(List.of("a", "b", "c"), List.of("d", "e"), 3)).containsExactly("c", "d", "e");
    }

    @Test
    void decode_shouldReturnEmptyForMissingHeader() {
        assertThat(MessageTracing.decode(null)).isEmpty();
        assertThat(MessageTracing.decode(MessageTracing.encode(List.of("a", "b")))).containsExactly("a", "b");
    }
}
