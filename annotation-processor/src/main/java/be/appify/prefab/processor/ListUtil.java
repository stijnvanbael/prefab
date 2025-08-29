package be.appify.prefab.processor;

import java.util.List;
import java.util.stream.Stream;

public class ListUtil {
    public static <T> List<T> concat(List<T> first, List<T> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
