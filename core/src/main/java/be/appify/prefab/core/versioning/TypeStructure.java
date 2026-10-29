package be.appify.prefab.core.versioning;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ResolvableType;

/** Navigates the generic structure of record types: collections, optionals, arrays and maps wrap an element type. */
final class TypeStructure {
    private TypeStructure() {
    }

    /**
     * Returns the type of the elements (or map values) wrapped by the given type.
     *
     * @param type the possibly wrapping type
     * @return the wrapped type, empty if the type does not wrap another type
     */
    static Optional<ResolvableType> elementType(ResolvableType type) {
        var raw = type.resolve(Object.class);
        if (raw.isArray()) {
            return Optional.of(type.getComponentType());
        }
        if (Optional.class.isAssignableFrom(raw)) {
            return Optional.of(type.getGeneric(0));
        }
        if (Map.class.isAssignableFrom(raw)) {
            return Optional.of(type.asMap().getGeneric(1));
        }
        if (Collection.class.isAssignableFrom(raw)) {
            return Optional.of(type.asCollection().getGeneric(0));
        }
        return Optional.empty();
    }
}
