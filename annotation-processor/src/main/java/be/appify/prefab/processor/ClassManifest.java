package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import org.springframework.data.annotation.Id;

/** Manifest representing a class in the Prefab domain model. */
public class ClassManifest {
    private static final Map<TypeElement, ClassManifest> manifestCache = new ConcurrentHashMap<>();

    private final ProcessingEnvironment processingEnvironment;
    private final List<VariableManifest> fields;
    private final VariableManifest parent;
    private final TypeManifest type;
    private final boolean isAggregate;
    private final TypeElement typeElement;
    private final VariableManifest idField;

    /**
     * Constructs a new ClassManifest.
     *
     * @param typeElement
     *         the type element representing the class
     * @param processingEnvironment
     *         the processing environment
     * @return the ClassManifest representing the given type element
     */
    public static ClassManifest of(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        return manifestCache.computeIfAbsent(typeElement, type -> new ClassManifest(type, processingEnvironment));
    }

    private ClassManifest(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.fields = getFields(typeElement);
        this.parent = getParent(fields);
        this.type = TypeManifest.of(typeElement.asType(), processingEnvironment);
        this.isAggregate = typeElement.getAnnotationsByType(Aggregate.class).length > 0;
        this.typeElement = typeElement;
        this.idField = getIdField();
        validate(typeElement);
    }

    private VariableManifest getIdField() {
        return fields.stream()
                .filter(field -> field.hasAnnotation(Id.class))
                .findFirst()
                .orElse(null);
    }

    private VariableManifest getParent(List<VariableManifest> fields) {
        var parents = fields.stream()
                .filter(field -> field.type().is(Reference.class) && field.hasAnnotation(Parent.class))
                .toList();
        if (parents.size() > 1) {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Class %s has multiple fields with @Parent annotation. Only one is allowed.".formatted(
                            qualifiedName()),
                    parents.get(1).element());
            return null;
        }
        return parents.isEmpty() ? null : parents.getFirst();
    }

    private void validate(TypeElement typeElement) {
        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Class %s must not be abstract".formatted(qualifiedName()),
                    typeElement);
        }
        boolean hasConstructor = typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .anyMatch(element -> {
                    var parameters = getParametersOf(element);
                    return fields.size() == parameters.size() && new HashSet<>(fields).containsAll(parameters);
                });
        if (isAggregate && idField == null) {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Aggregate %s is missing a field with @Id annotation".formatted(qualifiedName()),
                    typeElement
            );
        }
        if (!hasConstructor) {
            String message = """
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
                            .collect(Collectors.joining(";\n        ")));
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    message,
                    typeElement
            );
        }
    }

    private List<VariableManifest> getFields(TypeElement typeElement) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.FIELD
                        && !element.getModifiers().contains(Modifier.STATIC))
                .map(VariableElement.class::cast)
                .map(element -> VariableManifest.of(element, processingEnvironment))
                .toList();
    }

    private List<VariableManifest> getParametersOf(Element createConstructor) {
        return ((ExecutableElement) createConstructor).getParameters()
                .stream()
                .map(element -> VariableManifest.of(element, processingEnvironment))
                .toList();
    }

    /**
     * Gets the qualified name of the class.
     *
     * @return the qualified name
     */
    public String qualifiedName() {
        return packageName() + '.' + simpleName();
    }

    /**
     * Gets the package name of the class.
     *
     * @return the package name
     */
    public String packageName() {
        return type.packageName();
    }

    /**
     * Gets the simple name of the class.
     *
     * @return the simple name
     */
    public String simpleName() {
        return type.simpleName();
    }

    /**
     * Gets the fields of the class.
     *
     * @return the fields
     */
    public List<VariableManifest> fields() {
        return fields;
    }

    /**
     * Gets the type manifest of the class.
     *
     * @return the type manifest
     */
    public TypeManifest type() {
        return type;
    }

    /**
     * Gets the parent variable manifest, if any.
     *
     * @return the parent variable manifest
     */
    public Optional<VariableManifest> parent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Gets the class name of the class.
     *
     * @return the class name
     */
    public TypeName className() {
        return ClassName.get(packageName(), simpleName());
    }

    /**
     * Checks if the class is an aggregate.
     *
     * @return true if the class is an aggregate, false otherwise
     */
    public boolean isAggregate() {
        return isAggregate;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    /**
     * Gets the public constructors of the class that are annotated with the specified annotation.
     *
     * @param annotationType
     *         the annotation type
     * @return the list of constructors
     */
    public List<ExecutableElement> constructorsWith(Class<? extends Annotation> annotationType) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getAnnotationsByType(annotationType).length > 0
                        && element.getKind() == ElementKind.CONSTRUCTOR
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .toList();
    }

    /**
     * Gets the annotations of the specified type present on the class.
     *
     * @param annotationType
     *         the annotation type
     * @param <T>
     *         the type of the annotation
     * @return the set of annotations
     */
    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return Set.of(typeElement.getAnnotationsByType(annotationType));
    }

    /**
     * Gets the public methods of the class that are annotated with the specified annotation.
     *
     * @param annotation
     *         the annotation type
     * @return the list of methods
     */
    public List<ExecutableElement> methodsWith(Class<? extends Annotation> annotation) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(annotation).length > 0)
                .toList();
    }

    /**
     * Checks if the class depends on the specified class manifest.
     *
     * @param manifest
     *         the class manifest
     * @return true if the class depends on the specified manifest, false otherwise
     */
    public boolean dependsOn(ClassManifest manifest) {
        return fields.stream().anyMatch(field -> field.dependsOn(manifest.type));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        ClassManifest that = (ClassManifest) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    /**
     * Gets the ID field of the class, if any.
     *
     * @return the ID field
     */
    public Optional<VariableManifest> idField() {
        return Optional.ofNullable(idField);
    }
}
