package be.appify.prefab.processor;

import com.google.common.collect.Streams;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.springframework.util.ClassUtils;

/**
 * Represents a type manifest, encapsulating information about a type such as its package name, simple name, type parameters, and kind.
 */
public class TypeManifest {
    private final String packageName;
    private final String simpleName;
    private final ElementKind kind;
    private final List<TypeManifest> parameters;
    private TypeElement element;
    private final ProcessingEnvironment processingEnvironment;

    /**
     * Constructs a TypeManifest from a TypeMirror.
     *
     * @param typeMirror
     *         the TypeMirror representing the type
     * @param processingEnvironment
     *         the processing environment
     */
    public TypeManifest(TypeMirror typeMirror, ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        if (typeMirror.getKind().isPrimitive()) {
            this.packageName = "";
            this.simpleName = typeMirror.toString();
            this.parameters = List.of();
            this.kind = null;
        } else if (Objects.requireNonNull(typeMirror.getKind()) == TypeKind.DECLARED) {
            var declaredType = (DeclaredType) typeMirror;
            this.element = (TypeElement) declaredType.asElement();
            var fullyQualifiedName = element.getQualifiedName().toString();
            this.packageName = fullyQualifiedName.contains(".")
                    ? fullyQualifiedName.replaceAll("\\.[A-Z].+$", "")
                    : "";
            this.simpleName = packageName.isEmpty()
                    ? fullyQualifiedName
                    : fullyQualifiedName.substring(packageName.length() + 1);
            this.parameters = declaredType.getTypeArguments().stream()
                    .map(type -> new TypeManifest(type, processingEnvironment))
                    .toList();
            this.kind = element.getKind();
        } else {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Unsupported type: " + typeMirror
            );
            throw new IllegalArgumentException("Unsupported type: " + typeMirror);
        }
    }

    /**
     * Constructs a TypeManifest from a Class.
     *
     * @param type
     *         the Class representing the type
     * @param processingEnvironment
     *         the processing environment
     */
    public TypeManifest(Class<?> type, ProcessingEnvironment processingEnvironment) {
        this(
                type.getPackageName(),
                type.getSimpleName(),
                List.of(),
                ElementKind.CLASS,
                processingEnvironment
        );
    }

    /**
     * Constructs a TypeManifest with the specified attributes.
     *
     * @param packageName
     *         the package name of the type
     * @param simpleName
     *         the simple name of the type
     * @param parameters
     *         the type parameters of the type
     * @param kind
     *         the kind of the type
     * @param processingEnvironment
     *         the processing environment
     */
    public TypeManifest(String packageName, String simpleName, List<TypeManifest> parameters, ElementKind kind,
            ProcessingEnvironment processingEnvironment) {
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.parameters = parameters;
        this.kind = kind;
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * Creates a TypeManifest from a Class.
     *
     * @param clazz
     *         the Class representing the type
     * @param processingEnvironment
     *         the processing environment
     * @return a TypeManifest representing the specified class
     */
    public static TypeManifest of(Class<?> clazz, ProcessingEnvironment processingEnvironment) {
        return new TypeManifest(clazz.getPackageName(), clazz.getSimpleName(), List.of(), ElementKind.CLASS,
                processingEnvironment);
    }

    /**
     * Gets the package name of the type.
     *
     * @return the package name
     */
    public String packageName() {
        return packageName;
    }

    /**
     * Gets the simple name of the type.
     *
     * @return the simple name
     */
    public String simpleName() {
        return simpleName;
    }

    /**
     * Gets the type parameters of the type.
     *
     * @return the list of type parameters
     */
    public List<TypeManifest> parameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeManifest other
                && packageName.equals(other.packageName)
                && simpleName.equals(other.simpleName)
                && parameters.equals(other.parameters);
    }

    @Override
    public int hashCode() {
        return 31 * packageName.hashCode() + 17 * simpleName.hashCode() + 7 * parameters.hashCode();
    }

    @Override
    public String toString() {
        return "%s%s".formatted(
                simpleName,
                parameters.isEmpty() ? "" : "<%s>".formatted(parameters.stream().map(TypeManifest::toString).collect(
                        Collectors.joining(", "))));
    }

    /**
     * Checks if the type is an enum.
     *
     * @return true if the type is an enum, false otherwise
     */
    public boolean isEnum() {
        return kind == ElementKind.ENUM;
    }

    /**
     * Checks if the type matches the specified class.
     *
     * @param type
     *         the class to compare with
     * @return true if the type matches, false otherwise
     */
    public boolean is(Class<?> type) {
        return Objects.equals(packageName + "." + simpleName.replace('.', '$'), type.getName());
    }

    /**
     * Converts the TypeManifest to a TypeName.
     *
     * @return the TypeName representation of the type
     */
    public TypeName asTypeName() {
        if (packageName.isEmpty()) {
            return TypeName.get(asClass());
        } else if (parameters.isEmpty()) {
            return getClassName();
        }
        return ParameterizedTypeName.get(
                getClassName(),
                parameters.stream().map(TypeManifest::asTypeName).toArray(TypeName[]::new));
    }

    private ClassName getClassName() {
        boolean hasDot = simpleName.contains(".");
        return ClassName.get(packageName,
                hasDot ? simpleName.substring(0, simpleName.indexOf(".")) : simpleName,
                hasDot ? simpleName.substring(simpleName.indexOf(".") + 1).split("\\.") : new String[] {});
    }

    /**
     * Checks if the type is a standard Java type.
     *
     * @return true if the type is a standard Java type, false otherwise
     */
    public boolean isStandardType() {
        return packageName.isEmpty() || packageName.startsWith("java.");
    }

    public TypeManifest asBoxed() {
        return switch (simpleName()) {
            case "int" -> new TypeManifest(Integer.class, processingEnvironment);
            case "long" -> new TypeManifest(Long.class, processingEnvironment);
            case "double" -> new TypeManifest(Double.class, processingEnvironment);
            case "float" -> new TypeManifest(Float.class, processingEnvironment);
            case "boolean" -> new TypeManifest(Boolean.class, processingEnvironment);
            default -> this;
        };
    }

    /**
     * Converts the TypeManifest to a TypeElement.
     *
     * @return the TypeElement representation of the type
     */
    public TypeElement asElement() {
        return element;
    }

    /**
     * Checks if the type is a record.
     *
     * @return true if the type is a record, false otherwise
     */
    public boolean isRecord() {
        return kind == ElementKind.RECORD;
    }

    /**
     * Converts the TypeManifest to a ClassManifest.
     *
     * @return the ClassManifest representation of the type
     */
    public ClassManifest asClassManifest() {
        if (kind != ElementKind.CLASS && kind != ElementKind.RECORD) {
            throw new IllegalStateException("Type %s is not a class".formatted(this));
        }
        return new ClassManifest(asElement(), processingEnvironment);
    }

    /**
     * Converts the TypeManifest to a Class.
     *
     * @return the Class representation of the type
     */
    public Class<?> asClass() {
        try {
            if (packageName.isEmpty()) {
                return ClassUtils.forName(simpleName, TypeManifest.class.getClassLoader());
            }
            return TypeManifest.class.getClassLoader().loadClass(fullyQualifiedName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String fullyQualifiedName() {
        return packageName.isEmpty()
                ? simpleName
                : "%s.%s".formatted(packageName, simpleName.replace('.', '$'));
    }

    /**
     * Retrieves the annotations of the specified type present on the type.
     *
     * @param annotationType
     *         the class of the annotation type
     * @param <T>
     *         the type of the annotation
     * @return a set of annotations of the specified type
     */
    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return element != null ? Set.of(element.getAnnotationsByType(annotationType)) : Collections.emptySet();
    }

    /**
     * Retrieves the inherited annotations of the specified type from the type and its supertypes.
     *
     * @param annotationType
     *         the class of the annotation type
     * @param <T>
     *         the type of the annotation
     * @return a set of inherited annotations of the specified type
     */
    public <T extends Annotation> Set<T> inheritedAnnotationsOfType(Class<T> annotationType) {
        return Stream.concat(
                annotationsOfType(annotationType).stream(),
                supertypes().flatMap(superType -> superType.inheritedAnnotationsOfType(annotationType).stream())
        ).collect(Collectors.toSet());
    }

    private Stream<TypeManifest> supertypes() {
        return Stream.concat(
                Optional.of(element.getSuperclass())
                        .filter(e -> e.getKind() == TypeKind.DECLARED)
                        .map(e -> new TypeManifest(e, processingEnvironment)).stream(),
                element.getInterfaces().stream()
                        .filter(type -> type.getKind() == TypeKind.DECLARED)
                        .map(type -> new TypeManifest(type, processingEnvironment))
        );
    }

    /**
     * Finds the first supertype that has the specified annotation.
     *
     * @param annotationType
     *         the class of the annotation type
     * @return an Optional containing the supertype with the specified annotation, or empty if none found
     */
    public Optional<TypeManifest> supertypeWithAnnotation(Class<? extends Annotation> annotationType) {
        return supertypes()
                .filter(superType -> !superType.annotationsOfType(annotationType).isEmpty())
                .findFirst();
    }

    /**
     * Retrieves the public methods of the type that are annotated with the specified annotation.
     *
     * @param annotation
     *         the class of the annotation
     * @return a list of public methods annotated with the specified annotation
     */
    public List<ExecutableElement> methodsWith(Class<? extends Annotation> annotation) {
        return element.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(annotation).length > 0)
                .toList();
    }

    /**
     * Retrieves the fields of the type.
     *
     * @return a list of VariableManifest representing the fields of the type
     */
    public List<VariableManifest> fields() {
        return Streams.concat(
                        supertypes().flatMap(supertype -> supertype.fields().stream()),
                        element.getEnclosedElements()
                                .stream()
                                .filter(e -> e.getKind() == ElementKind.FIELD)
                                .map(VariableElement.class::cast)
                                .map(variableElement -> new VariableManifest(variableElement, processingEnvironment)))
                .toList();
    }

    /**
     * Retrieves the enum constant values of the type if it is an enum.
     *
     * @return a list of enum constant values, or an empty list if the type is not an enum
     */
    public List<String> enumValues() {
        return element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                .map(e -> e.getSimpleName().toString())
                .toList();
    }

    /**
     * Checks if the type is sealed.
     *
     * @return true if the type is sealed, false otherwise
     */
    public boolean isSealed() {
        return element != null && element.getModifiers().contains(Modifier.SEALED);
    }

    /**
     * Retrieves the permitted subtypes of the sealed interface.
     *
     * @return a list of TypeManifest representing the permitted subtypes
     */
    public List<TypeManifest> permittedSubtypes() {
        return element == null ? Collections.emptyList() : element.getPermittedSubclasses().stream()
                .map(type -> new TypeManifest(type, processingEnvironment))
                .toList();
    }
}
