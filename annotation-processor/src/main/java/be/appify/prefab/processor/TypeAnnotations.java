package be.appify.prefab.processor;

import java.lang.annotation.Annotation;
import java.util.Collections;
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

    TypeAnnotations(TypeElement element, ProcessingEnvironment processingEnvironment) {
        this.element = element;
        this.processingEnvironment = processingEnvironment;
    }

    <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return element != null ? Set.of(element.getAnnotationsByType(annotationType)) : Collections.emptySet();
    }

    <T extends Annotation> Set<T> inheritedAnnotationsOfType(Class<T> annotationType) {
        return Stream.concat(
                annotationsOfType(annotationType).stream(),
                supertypes().flatMap(superType -> superType.inheritedAnnotationsOfType(annotationType).stream())
        ).collect(Collectors.toSet());
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
