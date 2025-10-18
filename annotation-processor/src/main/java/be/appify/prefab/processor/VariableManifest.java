package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Optional;

public class VariableManifest {
    private final TypeManifest type;
    private final String name;
    private final List<AnnotationManifest> annotations;
    private final ProcessingEnvironment processingEnvironment;

    public VariableManifest(VariableElement variableElement, ProcessingEnvironment processingEnvironment) {
        this(
                new TypeManifest(variableElement.asType(), processingEnvironment),
                variableElement.getSimpleName().toString(),
                variableElement.getAnnotationMirrors().stream()
                        .map(annotationMirror -> new AnnotationManifest(annotationMirror, processingEnvironment))
                        .toList(),
                processingEnvironment
        );
    }

    public VariableManifest(TypeManifest type, String name, List<AnnotationManifest> annotations,
            ProcessingEnvironment processingEnvironment) {
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

    public List<AnnotationManifest> annotations() {
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
            case "int" -> new VariableManifest(TypeManifest.of(Integer.class, processingEnvironment), name, annotations,
                    processingEnvironment);
            case "long" -> new VariableManifest(TypeManifest.of(Long.class, processingEnvironment), name, annotations,
                    processingEnvironment);
            case "double" ->
                    new VariableManifest(TypeManifest.of(Double.class, processingEnvironment), name, annotations,
                            processingEnvironment);
            case "float" -> new VariableManifest(TypeManifest.of(Float.class, processingEnvironment), name, annotations,
                    processingEnvironment);
            case "boolean" ->
                    new VariableManifest(TypeManifest.of(Boolean.class, processingEnvironment), name, annotations,
                            processingEnvironment);
            case "char" ->
                    new VariableManifest(TypeManifest.of(Character.class, processingEnvironment), name, annotations,
                            processingEnvironment);
            default -> this;
        };
    }

    public ParameterSpec asParameterSpec() {
        return ParameterSpec.builder(type.asTypeName(), name)
                .build();
    }

    public VariableManifest withType(Class<?> type) {
        return new VariableManifest(TypeManifest.of(type, processingEnvironment), name, annotations,
                processingEnvironment);
    }

    public boolean hasAnnotation(Class<?> annotationClass) {
        return annotations.stream()
                .anyMatch(annotation -> annotation.type().is(annotationClass));
    }

    public Optional<AnnotationManifest> getAnnotation(Class<?> annotationClass) {
        return annotations.stream()
                .filter(annotation -> annotation.type().is(annotationClass))
                .findFirst();
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
