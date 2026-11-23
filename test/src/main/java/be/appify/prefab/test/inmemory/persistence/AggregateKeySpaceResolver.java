package be.appify.prefab.test.inmemory.persistence;

import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;

/**
 * Maps every aggregate type to the key space it is stored in. All implementations of a sealed aggregate share the
 * key space of the sealed root, so that the repository of the root finds every variant.
 */
public final class AggregateKeySpaceResolver implements KeySpaceResolver {

    /** Constructs a new AggregateKeySpaceResolver. */
    public AggregateKeySpaceResolver() {
    }

    @Override
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
