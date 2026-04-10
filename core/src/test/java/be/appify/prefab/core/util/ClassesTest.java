package be.appify.prefab.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassesTest {

    @Test
    void classWithNameReturnsCorrectClass() {
        var clazz = Classes.classWithName("java.lang.String");
        assertEquals(String.class, clazz);
    }

    @Test
    void classWithNameReturnsCorrectClassForPrimitive() {
        var clazz = Classes.classWithName("java.lang.Integer");
        assertEquals(Integer.class, clazz);
    }

    @Test
    void classWithNameThrowsForUnknownClass() {
        assertThrows(IllegalArgumentException.class, () ->
                Classes.classWithName("com.nonexistent.UnknownClass")
        );
    }

    @Test
    void classWithNameWorksForInnerClass() {
        var clazz = Classes.classWithName("java.util.Map$Entry");
        assertEquals(java.util.Map.Entry.class, clazz);
    }
}
