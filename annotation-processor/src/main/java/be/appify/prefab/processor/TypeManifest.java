package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.springframework.util.ClassUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        } else {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Unsupported type: " + typeMirror
            );
            packageName = null;
            simpleName = null;
            parameters = null;
            kind = null;
        }
    }

    public TypeManifest(Class<?> type, ProcessingEnvironment processingEnvironment) {
        this(
                type.getPackageName(),
                type.getSimpleName(),
                List.of(),
                ElementKind.CLASS,
                processingEnvironment
        );
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
        if(packageName.isEmpty()) {
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
        if(kind != ElementKind.CLASS && kind != ElementKind.RECORD) {
            throw new IllegalStateException("Type %s is not a class".formatted(this));
        }
        return new ClassManifest(asElement(), processingEnvironment);
    }

    public Class<?> asClass() {
        try {
            if(packageName.isEmpty()) {
                return ClassUtils.forName(simpleName, TypeManifest.class.getClassLoader());
            }
            return TypeManifest.class.getClassLoader()
                    .loadClass("%s.%s".formatted(packageName, simpleName.replace('.', '$')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return element != null ? Set.of(element.getAnnotationsByType(annotationType)) : Collections.emptySet();
    }

    public <T extends Annotation> Set<T> inheritedAnnotationsOfType(Class<T> annotationType) {
        return Stream.concat(
                annotationsOfType(annotationType).stream(),
                supertypes().flatMap(superType -> superType.inheritedAnnotationsOfType(annotationType).stream())
        ).collect(Collectors.toSet());
    }

    private Stream<TypeManifest> supertypes() {
        return Stream.concat(
                Optional.ofNullable(element.getSuperclass())
                        .filter(e -> e.getKind() == TypeKind.DECLARED)
                        .map(e -> new TypeManifest(e, processingEnvironment)).stream(),
                element.getInterfaces().stream()
                        .filter(type -> type.getKind() == TypeKind.DECLARED)
                        .map(type -> new TypeManifest(type, processingEnvironment))
        );
    }

    public Optional<TypeManifest> supertypeWithAnnotation(Class<? extends Annotation> annotationType) {
        return supertypes()
                .filter(superType -> !superType.annotationsOfType(annotationType).isEmpty())
                .findFirst();
    }

    public List<ExecutableElement> methodsWith(Class<? extends Annotation> annotation) {
        return element.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(annotation).length > 0)
                .toList();
    }
}
