package be.appify.prefab.processor;

import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class for list operations.
 */
public class ListUtil {

    private ListUtil() {
    }

    /**
     * Concatenates two lists into a single list.
     *
     * @param first  the first list
     * @param second the second list
     * @param <T>    the type of elements in the lists
     * @return a new list containing all elements from the first and second lists
     */
    public static <T> List<T> concat(List<T> first, List<T> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
