package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassManifest {
    private final ProcessingEnvironment processingEnvironment;
    private final List<VariableManifest> fields;
    private final VariableManifest parent;
    private final TypeManifest type;
    private final boolean isAggregate;
    private final TypeElement typeElement;

    public ClassManifest(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.fields = getFields(typeElement);
        this.parent = getParent(fields);
        this.type = new TypeManifest(typeElement.asType(), processingEnvironment);
        this.isAggregate = typeElement.getAnnotationsByType(Aggregate.class).length > 0;
        this.typeElement = typeElement;
        validate(typeElement);
    }

    @Deprecated
    public ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    private VariableManifest getParent(List<VariableManifest> fields) {
        var parents = fields.stream()
                .filter(field -> field.type().is(Reference.class) && field.hasAnnotation(Parent.class))
                .toList();
        if (parents.size() > 1) {
            throw new IllegalArgumentException(
                    "Class %s has multiple fields with @Parent annotation. Only one is allowed.".formatted(
                            qualifiedName()));
        }
        return parents.isEmpty() ? null : parents.getFirst();
    }

    private void validate(TypeElement typeElement) {
        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new IllegalArgumentException("Class %s must not be abstract".formatted(qualifiedName()));
        }
        boolean hasConstructor = typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR
                                   && element.getModifiers().contains(Modifier.PUBLIC))
                .anyMatch(element -> {
                    var parameters = getParametersOf(element);
                    return fields.size() == parameters.size() && new HashSet<>(fields).containsAll(parameters);
                });
        if (!hasConstructor) {
            throw new IllegalArgumentException(
                    """
                            Class %s must have a public constructor with all fields with exact types and names.
                            Suggested fix: add following constructor to %s:
                            
                                public %s(%s) {
                                    %s;
                                }
                            """.formatted(
                            qualifiedName(),
                            qualifiedName(),
                            simpleName(),
                            fields.stream()
                                    .map(field -> "%s %s".formatted(field.type().simpleName(), field.name()))
                                    .collect(Collectors.joining(", ")),
                            fields.stream()
                                    .map(field -> "this.%s = %s".formatted(field.name(), field.name()))
                                    .collect(Collectors.joining(";\n        "))));
        }
    }

    private List<VariableManifest> getFields(TypeElement typeElement) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.FIELD
                                   && !element.getModifiers().contains(Modifier.STATIC))
                .map(VariableElement.class::cast)
                .map(element -> new VariableManifest(element, processingEnvironment))
                .toList();
    }

    private List<VariableManifest> getParametersOf(Element createConstructor) {
        return ((ExecutableElement) createConstructor).getParameters()
                .stream()
                .map(element -> new VariableManifest(element, processingEnvironment))
                .toList();
    }

    public String qualifiedName() {
        return packageName() + '.' + simpleName();
    }

    public String packageName() {
        return type.packageName();
    }

    public String simpleName() {
        return type.simpleName();
    }

    public List<VariableManifest> fields() {
        return fields;
    }

    public TypeManifest type() {
        return type;
    }

    public Optional<VariableManifest> fieldByName(String property) {
        return fields.stream()
                .filter(field -> field.name().equals(property))
                .findFirst();
    }

    public Optional<VariableManifest> parent() {
        return Optional.ofNullable(parent);
    }

    public TypeName className() {
        return ClassName.get(packageName(), simpleName());
    }

    public boolean isAggregate() {
        return isAggregate;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public List<ExecutableElement> constructorsWith(Class<? extends Annotation> annotationType) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getAnnotationsByType(annotationType).length > 0
                                   && element.getKind() == ElementKind.CONSTRUCTOR
                                   && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .toList();
    }

    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return Set.of(typeElement.getAnnotationsByType(annotationType));
    }

    public List<ExecutableElement> methodsWith(Class<? extends Annotation> annotation) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                                   && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(annotation).length > 0)
                .toList();
    }

    public boolean dependsOn(ClassManifest manifest) {
        return fields.stream().anyMatch(field -> field.dependsOn(manifest.type));
    }
}
