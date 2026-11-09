package be.appify.prefab.processor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

/**
 * Handles annotation introspection for a type: direct annotations, inherited annotations,
 * and locating supertypes that carry a given annotation.
 */
class TypeAnnotations {

    private final TypeElement element;
    private final ProcessingEnvironment processingEnvironment;
    // Cache results of the recursive supertype walk, keyed by annotation type.
    private final Map<Class<?>, Set<?>> inheritedAnnotationCache = new HashMap<>();

    TypeAnnotations(TypeElement element, ProcessingEnvironment processingEnvironment) {
        this.element = element;
        this.processingEnvironment = processingEnvironment;
    }

    <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        // Insertion-ordered so that callers get deterministic results.
        return element != null
                ? new LinkedHashSet<>(Arrays.asList(element.getAnnotationsByType(annotationType)))
                : Collections.emptySet();
    }

    /**
     * Returns the type's own annotations followed by those inherited from its supertypes. The iteration order is
     * nearest-first, so a type's own annotation always precedes an annotation of the same type on a supertype.
     */
    @SuppressWarnings("unchecked")
    <T extends Annotation> Set<T> inheritedAnnotationsOfType(Class<T> annotationType) {
        return (Set<T>) inheritedAnnotationCache.computeIfAbsent(annotationType, type -> Stream.concat(
                annotationsOfType(annotationType).stream(),
                supertypes().flatMap(superType -> superType.inheritedAnnotationsOfType(annotationType).stream())
        ).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    Optional<TypeManifest> supertypeWithAnnotation(Class<? extends Annotation> annotationType) {
        return supertypes()
                .filter(superType -> !superType.annotationsOfType(annotationType).isEmpty())
                .findFirst();
    }

    private Stream<TypeManifest> supertypes() {
        if (element == null) {
            return Stream.empty();
        }
        return Stream.concat(
                Optional.of(element.getSuperclass())
                        .filter(e -> e.getKind() == TypeKind.DECLARED)
                        .map(e -> TypeManifest.of(e, processingEnvironment)).stream(),
                element.getInterfaces().stream()
                        .filter(type -> type.getKind() == TypeKind.DECLARED)
                        .map(type -> TypeManifest.of(type, processingEnvironment))
        );
    }
}
