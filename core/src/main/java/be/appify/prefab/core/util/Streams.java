package be.appify.prefab.core.util;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for working with streams.
 */
public class Streams {
    private Streams() {
    }

    /**
     * Creates a sequential {@code Stream} from an {@code Iterable}.
     *
     * @param iterable
     *         the source iterable
     * @param <T>
     *         the type of elements
     * @return a sequential {@code Stream}
     */
    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Creates a sequential {@code Stream} from an {@code Iterator}.
     *
     * @param iterator
     *         the source iterator
     * @param <T>
     *         the type of elements
     * @return a sequential {@code Stream}
     */
    public static <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(((Iterable<T>) () -> iterator).spliterator(), false);
    }
}
