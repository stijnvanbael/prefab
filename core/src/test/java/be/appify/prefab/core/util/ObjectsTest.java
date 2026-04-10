package be.appify.prefab.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObjectsTest {

    @Test
    void mapIfNotNullAppliesMapperWhenInputIsNotNull() {
        var result = Objects.mapIfNotNull("hello", String::toUpperCase);
        assertEquals("HELLO", result);
    }

    @Test
    void mapIfNotNullReturnsNullWhenInputIsNull() {
        var result = Objects.mapIfNotNull((String) null, String::toUpperCase);
        assertNull(result);
    }

    @Test
    void mapIfNotNullWorksWithIntegerInput() {
        var result = Objects.mapIfNotNull(5, n -> n * 2);
        assertEquals(10, result);
    }

    @Test
    void mapIfNotNullWorksWithNullIntegerInput() {
        Integer input = null;
        var result = Objects.mapIfNotNull(input, n -> n * 2);
        assertNull(result);
    }
}
