package be.appify.prefab.core.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamsTest {

    @Test
    void streamFromIterableReturnsAllElements() {
        Iterable<String> iterable = List.of("a", "b", "c");
        var result = Streams.stream(iterable).collect(Collectors.toList());
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void streamFromIterableIsSequential() {
        Iterable<Integer> iterable = List.of(1, 2, 3);
        var stream = Streams.stream(iterable);
        assertFalse(stream.isParallel());
    }

    @Test
    void streamFromEmptyIterableReturnsEmptyStream() {
        Iterable<String> iterable = List.of();
        var result = Streams.stream(iterable).collect(Collectors.toList());
        assertTrue(result.isEmpty());
    }

    @Test
    void streamFromIteratorReturnsAllElements() {
        Iterator<String> iterator = Arrays.asList("x", "y", "z").iterator();
        var result = Streams.stream(iterator).collect(Collectors.toList());
        assertEquals(List.of("x", "y", "z"), result);
    }

    @Test
    void streamFromIteratorIsSequential() {
        Iterator<Integer> iterator = Arrays.asList(1, 2, 3).iterator();
        var stream = Streams.stream(iterator);
        assertFalse(stream.isParallel());
    }

    @Test
    void streamFromEmptyIteratorReturnsEmptyStream() {
        Iterator<String> iterator = List.<String>of().iterator();
        var result = Streams.stream(iterator).collect(Collectors.toList());
        assertTrue(result.isEmpty());
    }
}
