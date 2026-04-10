package be.appify.prefab.processor;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Handles member introspection for a type: fields, methods annotated with a given annotation,
 * enum constants, sealed-type metadata, and permitted subtypes.
 */
class TypeMembers {

    private final TypeElement element;
    private final ProcessingEnvironment processingEnvironment;

    TypeMembers(TypeElement element, ProcessingEnvironment processingEnvironment) {
        this.element = element;
        this.processingEnvironment = processingEnvironment;
    }

    List<VariableManifest> fields() {
        if (element == null) {
            return Collections.emptyList();
        }
        return Stream.concat(
                        supertypes().flatMap(supertype -> supertype.fields().stream()),
                        element.getEnclosedElements()
                                .stream()
                                .filter(e -> e.getKind() == ElementKind.FIELD)
                                .map(VariableElement.class::cast)
                                .map(variableElement -> VariableManifest.of(variableElement, processingEnvironment)))
                .toList();
    }

    List<ExecutableElement> methodsWith(Class<? extends Annotation> annotation) {
        if (element == null) {
            return Collections.emptyList();
        }
        return element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD
                        && e.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(e -> e.getAnnotationsByType(annotation).length > 0)
                .toList();
    }

    List<String> enumValues() {
        if (element == null) {
            return Collections.emptyList();
        }
        return element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                .map(e -> e.getSimpleName().toString())
                .toList();
    }

    boolean isSealed() {
        return element != null && element.getModifiers().contains(Modifier.SEALED);
    }

    List<TypeManifest> permittedSubtypes() {
        return element == null ? Collections.emptyList() : element.getPermittedSubclasses().stream()
                .map(type -> TypeManifest.of(type, processingEnvironment))
                .toList();
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
