package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.CustomType;
import be.appify.prefab.core.annotations.Doc;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;
import org.springframework.util.ClassUtils;

/**
 * Represents a type manifest, encapsulating information about a type such as its package name, simple name, type parameters, and kind.
 * <p>
 * This class acts as a facade delegating to focused helpers:
 * <ul>
 *   <li>{@link TypeIdentity} – package/name/equality/code-generation helpers</li>
 *   <li>{@link TypeAnnotations} – annotation introspection</li>
 *   <li>{@link TypeMembers} – field/method/enum-constant introspection</li>
 * </ul>
 */
public class TypeManifest {
    private static final Map<Class<?>, TypeManifest> manifestByClassCache = new ConcurrentHashMap<>();
    private static final Map<TypeMirror, TypeManifest> manifestByTypeMirrorCache = new ConcurrentHashMap<>();

    private final TypeIdentity identity;
    private final TypeAnnotations annotations;
    private final TypeMembers members;
    private final ElementKind kind;
    private final TypeElement element;
    private final ProcessingEnvironment processingEnvironment;

    private TypeManifest(TypeMirror typeMirror, ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        if (typeMirror.getKind() == TypeKind.TYPEVAR) {
            var typeVariable = (TypeVariable) typeMirror;
            typeMirror = typeVariable.getUpperBound();
        }
        if (typeMirror.getKind().isPrimitive()) {
            this.identity = new TypeIdentity("", typeMirror.toString(), List.of());
            this.kind = null;
            this.element = null;
        } else if (typeMirror.getKind() == TypeKind.DECLARED || typeMirror.getKind() == TypeKind.ERROR) {
            var declaredType = (DeclaredType) typeMirror;
            this.element = (TypeElement) declaredType.asElement();
            var fullyQualifiedName = element.getQualifiedName().toString();
            // For ERROR types (unresolved), qualifiedName may be empty — fall back to the type's string form.
            if (fullyQualifiedName.isEmpty()) {
                fullyQualifiedName = typeMirror.toString();
            }
            var packageName = fullyQualifiedName.contains(".")
                    ? fullyQualifiedName.replaceAll("\\.[A-Z].+$", "")
                    : "";
            var simpleName = packageName.isEmpty()
                    ? fullyQualifiedName
                    : fullyQualifiedName.substring(packageName.length() + 1);
            var parameters = declaredType.getTypeArguments().stream()
                    .map(type -> new TypeManifest(type, processingEnvironment))
                    .toList();
            this.identity = new TypeIdentity(packageName, simpleName, parameters);
            this.kind = element.getKind();
        } else {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Unsupported type: " + typeMirror
            );
            throw new IllegalArgumentException("Unsupported type: " + typeMirror);
        }
        this.annotations = new TypeAnnotations(element, processingEnvironment);
        this.members = new TypeMembers(element, processingEnvironment);
    }

    private TypeManifest(String packageName, String simpleName, List<TypeManifest> parameters, ElementKind kind,
            ProcessingEnvironment processingEnvironment) {
        this.identity = new TypeIdentity(packageName, simpleName, parameters);
        this.kind = kind;
        this.element = null;
        this.processingEnvironment = processingEnvironment;
        this.annotations = new TypeAnnotations(null, processingEnvironment);
        this.members = new TypeMembers(null, processingEnvironment);
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
        return manifestByClassCache.computeIfAbsent(clazz,
                c -> new TypeManifest(c.getPackageName(), c.getSimpleName(), List.of(), ElementKind.CLASS, processingEnvironment));
    }

    /**
     * Creates a TypeManifest from a TypeMirror.
     *
     * @param typeMirror
     *         the TypeMirror representing the type
     * @param processingEnvironment
     *         the processing environment
     * @return a TypeManifest representing the specified type mirror
     */
    public static TypeManifest of(TypeMirror typeMirror, ProcessingEnvironment processingEnvironment) {
        if (typeMirror.getKind() == TypeKind.ERROR) {
            // Unresolved type (generated by another processor in the same round).
            // Do not cache — it will be re-resolved as DECLARED in the next round.
            return new TypeManifest(typeMirror, processingEnvironment);
        }
        return manifestByTypeMirrorCache.computeIfAbsent(typeMirror, type -> new TypeManifest(type, processingEnvironment));
    }

    /**
     * Returns true if {@code mirror} is, or transitively contains, an unresolved ({@code ERROR}-kind) type. This covers cases like
     * {@code List<LifecycleSnapshot>} where the outer type is {@code DECLARED} but a type argument is still unresolved.
     */
    public static boolean containsUnresolvedType(TypeMirror mirror) {
        return switch (mirror.getKind()) {
            case ERROR -> true;
            case DECLARED -> ((DeclaredType) mirror).getTypeArguments().stream()
                    .anyMatch(TypeManifest::containsUnresolvedType);
            default -> false;
        };
    }

    public String packageName() {
        return identity.packageName();
    }

    public String simpleName() {
        return identity.simpleName();
    }

    public List<TypeManifest> parameters() {
        return identity.parameters();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeManifest other && identity.equals(other.identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    @Override
    public String toString() {
        return identity.toString();
    }

    public boolean isEnum() {
        return kind == ElementKind.ENUM;
    }

    public boolean is(Class<?> type) {
        return identity.is(type);
    }

    public TypeName asTypeName() {
        return identity.asTypeName();
    }

    public boolean isStandardType() {
        return identity.isStandardType();
    }

    /**
     * Converts a primitive type to its boxed equivalent; returns itself for non-primitives.
     */
    public TypeManifest asBoxed() {
        return switch (simpleName()) {
            case "int" -> TypeManifest.of(Integer.class, processingEnvironment);
            case "long" -> TypeManifest.of(Long.class, processingEnvironment);
            case "double" -> TypeManifest.of(Double.class, processingEnvironment);
            case "float" -> TypeManifest.of(Float.class, processingEnvironment);
            case "boolean" -> TypeManifest.of(Boolean.class, processingEnvironment);
            case "char" -> TypeManifest.of(Character.class, processingEnvironment);
            default -> this;
        };
    }

    public TypeElement asElement() {
        return element;
    }

    public boolean isRecord() {
        return kind == ElementKind.RECORD;
    }

    public ClassManifest asClassManifest() {
        if (kind != ElementKind.CLASS && kind != ElementKind.RECORD) {
            throw new IllegalStateException("Type %s is not a class".formatted(this));
        }
        return ClassManifest.of(asElement(), processingEnvironment);
    }

    /**
     * Converts the TypeManifest to a Class.
     *
     * @return the Class representation of the type
     */
    public Class<?> asClass() {
        try {
            if (identity.packageName().isEmpty()) {
                return ClassUtils.forName(identity.simpleName(), TypeManifest.class.getClassLoader());
            }
            return TypeManifest.class.getClassLoader().loadClass(identity.fullyQualifiedName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        return annotations.annotationsOfType(annotationType);
    }

    /**
     * Returns the human-readable description from a type-level {@code @Doc} annotation, if present.
     *
     * @return an Optional containing the description, or empty if no {@code @Doc} is present
     */
    public Optional<String> doc() {
        return annotationsOfType(Doc.class).stream().findFirst().map(Doc::value);
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
        return annotations.inheritedAnnotationsOfType(annotationType);
    }

    /**
     * Finds the first supertype that has the specified annotation.
     *
     * @param annotationType
     *         the class of the annotation type
     * @return an Optional containing the supertype with the specified annotation, or empty if none found
     */
    public Optional<TypeManifest> supertypeWithAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.supertypeWithAnnotation(annotationType);
    }

    /**
     * Retrieves the public methods of the type that are annotated with the specified annotation.
     *
     * @param annotation
     *         the class of the annotation
     * @return a list of public methods annotated with the specified annotation
     */
    public List<ExecutableElement> methodsWith(Class<? extends Annotation> annotation) {
        return members.methodsWith(annotation);
    }

    /**
     * Retrieves the fields of the type.
     *
     * @return a list of VariableManifest representing the fields of the type
     */
    public List<VariableManifest> fields() {
        return members.fields();
    }

    /**
     * Retrieves the enum constant values of the type if it is an enum.
     *
     * @return a list of enum constant values, or an empty list if the type is not an enum
     */
    public List<String> enumValues() {
        return members.enumValues();
    }

    /**
     * Checks if the type is sealed.
     *
     * @return true if the type is sealed, false otherwise
     */
    public boolean isSealed() {
        return members.isSealed();
    }

    /**
     * Retrieves the permitted subtypes of the sealed interface.
     *
     * @return a list of TypeManifest representing the permitted subtypes
     */
    public List<TypeManifest> permittedSubtypes() {
        return members.permittedSubtypes();
    }

    /**
     * Checks if the type is a single-value type, i.e. a record with exactly one record component.
     * <p>
     * Any record with a single component is automatically treated as a scalar value wrapper by the Prefab framework. The {@link #fields()}
     * method returns the record's backing fields, which correspond 1:1 to record components.
     * </p>
     *
     * @return true if the type is a record with exactly one component, false otherwise
     */
    public boolean isSingleValueType() {
        return isRecord() && fields().size() == 1;
    }

    /**
     * Returns the name of the accessor method for the single value of this type. For records, the accessor method has the same name as the
     * single record component.
     *
     * @return the accessor method name
     * @throws IllegalStateException
     *         if the type is not a single-value type
     */
    public String singleValueAccessor() {
        if (!isSingleValueType()) {
            throw new IllegalStateException("Type %s is not a single value type".formatted(this));
        }
        return fields().getFirst().name();
    }

    /**
     * Checks whether the type is annotated with {@link CustomType}, meaning the Prefab annotation processor should not attempt automatic
     * database-column or Avro-field mapping for fields of this type.
     *
     * @return {@code true} if the type declares {@code @CustomType}, {@code false} otherwise
     */
    public boolean isCustomType() {
        return !annotationsOfType(CustomType.class).isEmpty();
    }
}
