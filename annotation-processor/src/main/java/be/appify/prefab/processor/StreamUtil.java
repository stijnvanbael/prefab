package be.appify.prefab.processor;

import java.util.stream.Stream;

public class StreamUtil {
    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T> first, Stream<T> second, Stream<T>... others) {
        Stream<T> result = Stream.concat(first, second);
        for (Stream<T> other : others) {
            result = Stream.concat(result, other);
        }
        return result;
    }
}
