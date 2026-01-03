package be.appify.prefab.core.util;

import java.util.function.Function;

/**
 * Utility class for object-related operations.
 */
public abstract class Objects {
    private Objects() {
    }

    /**
     * Maps the input to the output using the provided mapper function if the input is not null.
     *
     * @param input
     *     the input to map
     * @param mapper
     *     the mapper function
     * @param <I>
     *     the input type
     * @param <O>
     *     the output type
     * @return the output of the mapper function if the input is not null, otherwise null
     */
    public static <I, O> O mapIfNotNull(I input, Function<I, O> mapper) {
        if (input == null) {
            return null;
        }
        return mapper.apply(input);
    }
}
