package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

/**
 * Manifest representing a variable (field, method parameter, etc.) in the code being processed.
 */
public class VariableManifest {
    private final VariableElement element;
    private final TypeManifest type;
    private final String name;
    private final List<? extends AnnotationManifest<?>> annotations;
    private final ProcessingEnvironment processingEnvironment;

    /**
     * Constructs a VariableManifest from a VariableElement.
     *
     * @param variableElement       the variable element to construct the manifest from
     * @param processingEnvironment the processing environment
     */
    public VariableManifest(VariableElement variableElement, ProcessingEnvironment processingEnvironment) {
        this(
                variableElement,
                new TypeManifest(variableElement.asType(), processingEnvironment),
                variableElement.getSimpleName().toString(),
                variableElement.getAnnotationMirrors().stream()
                        .map(annotationMirror ->
                                new AnnotationManifest<>(annotationMirror,
                                        processingEnvironment,
                                        resolveAnnotation(variableElement, annotationMirror, processingEnvironment)))
                        .toList(),
                processingEnvironment
        );
    }

    @SuppressWarnings("unchecked")
    private static Annotation resolveAnnotation(
            VariableElement variableElement,
            AnnotationMirror annotationMirror,
            ProcessingEnvironment processingEnvironment
    ) {
        return variableElement.getAnnotation((Class<? extends Annotation>) new TypeManifest(
                annotationMirror.getAnnotationType().asElement().asType(),
                processingEnvironment).asClass());
    }

    /**
     * Constructs a VariableManifest with the given properties.
     *
     * @param element              the variable element
     * @param type                 the type manifest of the variable
     * @param name                 the name of the variable
     * @param annotations          the list of annotation manifests on the variable
     * @param processingEnvironment the processing environment
     */
    public VariableManifest(
            VariableElement element,
            TypeManifest type,
            String name,
            List<? extends AnnotationManifest<?>> annotations,
            ProcessingEnvironment processingEnvironment
    ) {
        this.element = element;
        this.type = type;
        this.name = name;
        this.annotations = annotations;
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * Gets the type manifest of the variable.
     *
     * @return the type manifest
     */
    public TypeManifest type() {
        return type;
    }

    /**
     * Gets the name of the variable.
     *
     * @return the variable name
     */
    public String name() {
        return name;
    }

    /**
     * Gets the underlying VariableElement.
     *
     * @return the variable element
     */
    public VariableElement element() {
        return element;
    }

    /**
     * Gets the list of annotation manifests on the variable.
     *
     * @return the list of annotation manifests
     */
    public List<? extends AnnotationManifest<?>> annotations() {
        return annotations;
    }

    /**
     * Determines if the variable is nullable.
     *
     * @return true if the variable is nullable, false otherwise
     */
    public boolean nullable() {
        return !isPrimitive() && annotations.stream()
                .noneMatch(annotation -> annotation.type().is(NotNull.class) || annotation.type().is(Id.class));
    }

    @Override
    public String toString() {
        return type + " " + name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableManifest other
                && type.equals(other.type)
                && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return 31 * type().hashCode() + 17 * name().hashCode();
    }

    /**
     * Converts the variable's type to its boxed equivalent if it is a primitive type.
     *
     * @return a VariableManifest with the boxed type, or the same manifest if already boxed
     */
    public VariableManifest toBoxed() {
        return switch (type.simpleName()) {
            case "int" -> new VariableManifest(element, TypeManifest.of(Integer.class, processingEnvironment), name,
                    annotations,
                    processingEnvironment);
            case "long" ->
                    new VariableManifest(element, TypeManifest.of(Long.class, processingEnvironment), name, annotations,
                            processingEnvironment);
            case "double" -> new VariableManifest(element, TypeManifest.of(Double.class, processingEnvironment), name,
                    annotations,
                    processingEnvironment);
            case "float" -> new VariableManifest(element, TypeManifest.of(Float.class, processingEnvironment), name,
                    annotations,
                    processingEnvironment);
            case "boolean" -> new VariableManifest(element, TypeManifest.of(Boolean.class, processingEnvironment), name,
                    annotations,
                    processingEnvironment);
            case "char" -> new VariableManifest(element, TypeManifest.of(Character.class, processingEnvironment), name,
                    annotations,
                    processingEnvironment);
            default -> this;
        };
    }

    /**
     * Creates a new VariableManifest with the specified type.
     *
     * @param type the new type class
     * @return a VariableManifest with the new type
     */
    public VariableManifest withType(Class<?> type) {
        return new VariableManifest(element, TypeManifest.of(type, processingEnvironment), name, annotations,
                processingEnvironment);
    }

    /**
     * Checks if the variable has the specified annotation.
     *
     * @param annotationClass the annotation class to check for
     * @return true if the annotation is present, false otherwise
     */
    public boolean hasAnnotation(Class<?> annotationClass) {
        return annotations.stream()
                .anyMatch(annotation -> annotation.type().is(annotationClass));
    }

    /**
     * Gets the annotation manifest for the specified annotation class.
     *
     * @param annotationClass the annotation class to retrieve
     * @param <A>             the type of the annotation
     * @return an Optional containing the AnnotationManifest if present, or empty if not
     */
    public <A extends Annotation> Optional<AnnotationManifest<A>> getAnnotation(Class<A> annotationClass) {
        return annotations.stream()
                .filter(annotation -> annotation.type().is(annotationClass))
                .findFirst()
                .map(annotation -> (AnnotationManifest<A>) annotation);
    }

    /**
     * Determines if the variable's type depends on the specified type.
     *
     * @param type the type manifest to check dependency against
     * @return true if the variable's type depends on the specified type, false otherwise
     */
    public boolean dependsOn(TypeManifest type) {
        return this.type == type
                || (this.type.isRecord() && this.type.asClassManifest().fields().stream()
                .anyMatch(field -> field.dependsOn(type)))
                || (this.type.is(Reference.class) && this.type.parameters().getFirst() == type)
                || (this.type.is(List.class) && this.type.parameters().getFirst().asClassManifest()
                .dependsOn(type.asClassManifest()));
    }

    /**
     * Determines if the variable's type is a primitive type.
     *
     * @return true if the type is primitive, false otherwise
     */
    public boolean isPrimitive() {
        return type.packageName().isEmpty() && switch (type.simpleName()) {
            case "int", "long", "double", "float", "boolean", "char", "byte", "short" -> true;
            default -> false;
        };
    }
}
