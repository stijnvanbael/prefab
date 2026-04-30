package be.appify.prefab.processor;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
                        backingFields())
                .toList();
    }

    private Stream<VariableManifest> backingFields() {
        if (element.getKind() != ElementKind.RECORD) {
            return element.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .map(VariableElement.class::cast)
                    .map(field -> VariableManifest.of(field, processingEnvironment));
        }
        return recordFieldsWithComponentAnnotations();
    }

    /**
     * For records, merges backing-field annotations with record-component annotations.
     * Some compilers (e.g. IntelliJ's incremental compiler) do not propagate record-component
     * annotations to the backing field. Reading both sources and merging ensures annotations
     * such as {@code @Nullable} are always visible regardless of the compilation context.
     */
    private Stream<VariableManifest> recordFieldsWithComponentAnnotations() {
        var componentAnnotations = recordComponentAnnotationsByName();
        return element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .map(field -> VariableManifest.of(field, processingEnvironment)
                        .withAdditionalAnnotations(
                                componentAnnotations.getOrDefault(field.getSimpleName().toString(), List.of())));
    }

    private Map<String, List<? extends AnnotationManifest<?>>> recordComponentAnnotationsByName() {
        return element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.RECORD_COMPONENT)
                .map(VariableElement.class::cast)
                .collect(Collectors.toMap(
                        comp -> comp.getSimpleName().toString(),
                        comp -> VariableManifest.of(comp, processingEnvironment).annotations()));
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
                .filter(type -> type.getKind() != TypeKind.ERROR)
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
