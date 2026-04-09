package be.appify.prefab.processor;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListUtilTest {

    @Test
    void concatCombinesTwoLists() {
        var first = List.of("a", "b");
        var second = List.of("c", "d");
        assertEquals(List.of("a", "b", "c", "d"), ListUtil.concat(first, second));
    }

    @Test
    void concatPreservesOrderFirstThenSecond() {
        var first = List.of(1, 2, 3);
        var second = List.of(4, 5, 6);
        var result = ListUtil.concat(first, second);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    void concatWithEmptyFirstList() {
        var first = List.<String>of();
        var second = List.of("a", "b");
        assertEquals(List.of("a", "b"), ListUtil.concat(first, second));
    }

    @Test
    void concatWithEmptySecondList() {
        var first = List.of("a", "b");
        var second = List.<String>of();
        assertEquals(List.of("a", "b"), ListUtil.concat(first, second));
    }

    @Test
    void concatWithBothEmptyLists() {
        var first = List.<String>of();
        var second = List.<String>of();
        assertTrue(ListUtil.concat(first, second).isEmpty());
    }
}
