package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/**
 * Shared utilities for resolving and validating {@link PartitioningKey} methods used by event contracts.
 */
public final class PartitioningKeySupport {

    private PartitioningKeySupport() {
    }

    /**
     * Returns whether the event contract declares an annotated partitioning-key method on itself or its supertypes.
     */
    public static boolean hasPartitioningKey(TypeManifest event) {
        return findPartitioningKeyMethod(event).isPresent();
    }

    /**
     * Resolves the effective partitioning-key method and the code needed to invoke it from generated registrars.
     */
    public static Optional<PartitioningKeyMethod> partitioningKey(TypeManifest event, PrefabContext context) {
        return findPartitioningKeyMethod(event)
                .flatMap(method -> buildPartitioningKeyMethod(event, method, context));
    }

    /**
     * Returns abstract no-argument contract methods that AVSC-generated records must satisfy with schema fields.
     */
    public static List<ExecutableElement> abstractContractMethods(TypeManifest event) {
        var methods = new LinkedHashMap<String, ExecutableElement>();
        collectAnnotatedMethods(event, methods, new LinkedHashSet<>(), false);
        return methods.values().stream()
                .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                .filter(method -> method.getParameters().isEmpty())
                .filter(method -> method.getReturnType().getKind() != TypeKind.VOID)
                .toList();
    }

    private static Optional<ExecutableElement> findPartitioningKeyMethod(TypeManifest event) {
        var methods = new LinkedHashMap<String, ExecutableElement>();
        collectAnnotatedMethods(event, methods, new LinkedHashSet<>(), true);
        return methods.values().stream().findFirst();
    }

    private static void collectAnnotatedMethods(TypeManifest type, LinkedHashMap<String, ExecutableElement> methods,
                                                Set<String> visited, boolean partitioningKeyOnly) {
        var element = type.asElement();
        if (element == null || !visited.add(element.getQualifiedName().toString())) {
            return;
        }
        if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE || element.getKind() == ElementKind.RECORD) {
            element.getEnclosedElements().stream()
                    .filter(member -> member.getKind() == ElementKind.METHOD)
                    .map(ExecutableElement.class::cast)
                    .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                    .filter(method -> !partitioningKeyOnly || method.getAnnotation(PartitioningKey.class) != null)
                    .forEach(method -> methods.putIfAbsent(methodSignature(method), method));
        }
        directSupertypes(type).forEach(supertype -> collectAnnotatedMethods(supertype, methods, visited, partitioningKeyOnly));
    }

    private static Stream<TypeManifest> directSupertypes(TypeManifest type) {
        var element = type.asElement();
        if (element == null) {
            return Stream.empty();
        }
        var processingEnvironment = type.processingEnvironment();
        var supertypes = new ArrayList<TypeManifest>();
        var superclass = element.getSuperclass();
        if (superclass.getKind() == TypeKind.DECLARED) {
            supertypes.add(TypeManifest.of(superclass, processingEnvironment));
        }
        element.getInterfaces().stream()
                .filter(interfaceType -> interfaceType.getKind() == TypeKind.DECLARED)
                .map(interfaceType -> TypeManifest.of(interfaceType, processingEnvironment))
                .forEach(supertypes::add);
        return supertypes.stream();
    }

    private static Optional<PartitioningKeyMethod> buildPartitioningKeyMethod(TypeManifest event, ExecutableElement method,
                                                                              PrefabContext context) {
        if (!method.getParameters().isEmpty()) {
            context.logError("@PartitioningKey method '%s()' on %s must not declare parameters."
                    .formatted(method.getSimpleName(), event.simpleName()), method);
            return Optional.empty();
        }
        var returnType = TypeManifest.of(method.getReturnType(), context.processingEnvironment());
        if (returnType.is(String.class)) {
            return Optional.of(new PartitioningKeyMethod(method, CodeBlock.of("event.$L()", method.getSimpleName()),
                    isSynthetic(method)));
        }
        if (returnType.isSingleValueType()) {
            var valueField = returnType.fields().stream().findFirst();
            if (valueField.isPresent() && valueField.get().type().asBoxed().is(String.class)) {
                return Optional.of(new PartitioningKeyMethod(method,
                        CodeBlock.of("event.$L().$L()", method.getSimpleName(), returnType.singleValueAccessor()),
                        isSynthetic(method)));
            }
        }
        context.logError("@PartitioningKey method '%s()' must return String or a single-value type backed by String."
                .formatted(method.getSimpleName()), method);
        return Optional.empty();
    }

    private static boolean isSynthetic(ExecutableElement method) {
        return !method.getModifiers().contains(Modifier.ABSTRACT);
    }

    private static String methodSignature(ExecutableElement method) {
        var parameterTypes = method.getParameters().stream()
                .map(parameter -> parameter.asType().toString())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return method.getSimpleName() + "(" + parameterTypes + ")";
    }

    /** Resolved partitioning-key metadata for a contract method. */
    public record PartitioningKeyMethod(ExecutableElement method, CodeBlock extractor, boolean synthetic) {
        /** Returns the accessor name used when the partitioning key is schema-backed. */
        public String propertyName() {
            return method.getSimpleName().toString();
        }
    }
}
