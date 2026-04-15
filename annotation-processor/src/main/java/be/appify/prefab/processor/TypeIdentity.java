package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates the identity of a type: package name, simple name, type parameters, equality,
 * fully-qualified name, and code-generation helpers such as {@code asTypeName()}.
 */
class TypeIdentity {

    private final String packageName;
    private final String simpleName;
    private final List<TypeManifest> parameters;

    TypeIdentity(String packageName, String simpleName, List<TypeManifest> parameters) {
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.parameters = parameters;
    }

    String packageName() {
        return packageName;
    }

    String simpleName() {
        return simpleName;
    }

    List<TypeManifest> parameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeIdentity other
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

    String fullyQualifiedName() {
        return packageName.isEmpty()
                ? simpleName
                : "%s.%s".formatted(packageName, simpleName.replace('.', '$'));
    }

    private static final Set<String> PRIMITIVE_NAMES = Set.of(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void");

    TypeName asTypeName() {
        if (packageName.isEmpty()) {
            // Only primitive types legitimately have an empty package.
            // An unresolved (ERROR-kind) type may also land here when its package is unknown;
            // return a best-effort ClassName rather than crashing with ClassNotFoundException.
            if (PRIMITIVE_NAMES.contains(simpleName)) {
                return TypeName.get(asClass());
            }
            return ClassName.get("", simpleName);
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

    boolean isStandardType() {
        return packageName.isEmpty() || packageName.startsWith("java.");
    }

    boolean is(Class<?> type) {
        String fqn = packageName.isEmpty()
                ? simpleName.replace('.', '$')
                : packageName + "." + simpleName.replace('.', '$');
        return Objects.equals(fqn, type.getName());
    }

    private Class<?> asClass() {
        try {
            return org.springframework.util.ClassUtils.forName(simpleName, TypeIdentity.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
