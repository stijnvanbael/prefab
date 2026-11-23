package be.appify.prefab.test.inmemory.persistence;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Maps every aggregate type to the key space it is stored in. All implementations of a sealed aggregate share the
 * key space of the sealed root, so that the repository of the root finds every variant.
 */
public final class AggregateKeySpaceResolver {

    /** Constructs a new AggregateKeySpaceResolver. */
    public AggregateKeySpaceResolver() {
    }

    /**
     * Resolves the key space of an aggregate type.
     *
     * @param type
     *         the aggregate type, a variant or the sealed root
     * @return the key space name
     */
    public String resolveKeySpace(Class<?> type) {
        var root = type;
        for (var parent = sealedParentOf(root); parent.isPresent(); parent = sealedParentOf(root)) {
            root = parent.get();
        }
        return root.getName();
    }

    private static Optional<Class<?>> sealedParentOf(Class<?> type) {
        return Stream.concat(Stream.ofNullable(type.getSuperclass()), Stream.of(type.getInterfaces()))
                .filter(Class::isSealed)
                .findFirst();
    }
}
