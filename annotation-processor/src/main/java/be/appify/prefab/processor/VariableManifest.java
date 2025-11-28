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

public class VariableManifest {
    private final VariableElement element;
    private final TypeManifest type;
    private final String name;
    private final List<? extends AnnotationManifest<?>> annotations;
    private final ProcessingEnvironment processingEnvironment;

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

    private static Annotation resolveAnnotation(
            VariableElement variableElement,
            AnnotationMirror annotationMirror,
            ProcessingEnvironment processingEnvironment
    ) {
        return variableElement.getAnnotation((Class<? extends Annotation>) new TypeManifest(
                annotationMirror.getAnnotationType().asElement().asType(),
                processingEnvironment).asClass());
    }

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

    public TypeManifest type() { // TODO: replace TypeManifest with TypeName
        return type;
    }

    public String name() {
        return name;
    }

    public VariableElement element() {
        return element;
    }

    public List<? extends AnnotationManifest<?>> annotations() {
        return annotations;
    }

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

    public VariableManifest withType(Class<?> type) {
        return new VariableManifest(element, TypeManifest.of(type, processingEnvironment), name, annotations,
                processingEnvironment);
    }

    public boolean hasAnnotation(Class<?> annotationClass) {
        return annotations.stream()
                .anyMatch(annotation -> annotation.type().is(annotationClass));
    }

    public <A extends Annotation> Optional<AnnotationManifest<A>> getAnnotation(Class<A> annotationClass) {
        return annotations.stream()
                .filter(annotation -> annotation.type().is(annotationClass))
                .findFirst()
                .map(annotation -> (AnnotationManifest<A>) annotation);
    }

    public boolean dependsOn(TypeManifest type) {
        return this.type == type
                || (this.type.isRecord() && this.type.asClassManifest().fields().stream()
                .anyMatch(field -> field.dependsOn(type)))
                || (this.type.is(Reference.class) && this.type.parameters().getFirst() == type)
                || (this.type.is(List.class) && this.type.parameters().getFirst().asClassManifest()
                .dependsOn(type.asClassManifest()));
    }

    public boolean isPrimitive() {
        return type.packageName().isEmpty() && switch (type.simpleName()) {
            case "int", "long", "double", "float", "boolean", "char", "byte", "short" -> true;
            default -> false;
        };
    }
}
