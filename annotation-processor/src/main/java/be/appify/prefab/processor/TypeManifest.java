package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeManifest {
    private final String packageName;
    private final String simpleName;
    private final ElementKind kind;
    private final List<TypeManifest> parameters;
    private TypeElement element;
    private final ProcessingEnvironment processingEnvironment;

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
            this.packageName = element.getQualifiedName().toString()
                    .replaceAll("\\.[A-Z].+$", "");
            this.simpleName = element.getQualifiedName().toString().substring(packageName.length() + 1);
            this.parameters = declaredType.getTypeArguments().stream()
                    .map(type -> new TypeManifest(type, processingEnvironment))
                    .toList();
            this.kind = element.getKind();
        } else if (Objects.requireNonNull(typeMirror.getKind()) == TypeKind.ARRAY) {
            var arrayType = ((ArrayType) typeMirror).getComponentType();
            if (arrayType.getKind() != TypeKind.BYTE) {
                throw new IllegalArgumentException("Only byte is supported for arrays, found: " + arrayType);
            }
            this.element = null;
            this.packageName = "java.lang";
            this.simpleName = "byte[]";
            this.parameters = Collections.emptyList();
            this.kind = ElementKind.CLASS;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + typeMirror);
        }
    }

    public TypeManifest(String packageName, String simpleName, List<TypeManifest> parameters, ElementKind kind,
            ProcessingEnvironment processingEnvironment) {
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.parameters = parameters;
        this.kind = kind;
        this.processingEnvironment = processingEnvironment;
    }

    public static TypeManifest of(Class<?> clazz, ProcessingEnvironment processingEnvironment) {
        return new TypeManifest(clazz.getPackageName(), clazz.getSimpleName(), List.of(), ElementKind.CLASS,
                processingEnvironment);
    }

    public String packageName() {
        return packageName;
    }

    public String simpleName() {
        return simpleName;
    }

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

    public boolean isEnum() {
        return kind == ElementKind.ENUM;
    }

    public boolean is(Class<?> type) {
        return packageName.equals(type.getPackageName()) && simpleName.equals(type.getSimpleName());
    }

    public TypeName asTypeName() {
        return asTypeName("%s", "%s");
    }

    public TypeName asTypeName(String packageFormat, String nameFormat) {
        if (parameters.isEmpty()) {
            return ClassName.get(packageFormat.formatted(packageName), nameFormat.formatted(simpleName));
        }
        return ParameterizedTypeName.get(
                ClassName.get(packageFormat.formatted(packageName), nameFormat.formatted(simpleName)),
                parameters.stream().map(TypeManifest::asTypeName).toArray(TypeName[]::new));
    }

    public boolean isStandardType() {
        return packageName.isEmpty() || packageName.startsWith("java.");
    }

    public TypeElement asElement() {
        return element;
    }

    public boolean isRecord() {
        return kind == ElementKind.RECORD;
    }

    public ClassManifest asClassManifest() {
        return new ClassManifest(asElement(), processingEnvironment);
    }

    public Class<?> asClass() {
        try {
            return TypeManifest.class.getClassLoader()
                    .loadClass("%s.%s".formatted(packageName, simpleName.replace('.', '$')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return Set.of(element.getAnnotationsByType(annotationType));
    }
}
