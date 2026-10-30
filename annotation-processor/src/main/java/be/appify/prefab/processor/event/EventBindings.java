package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.serialization.EventSubscriptionNames;
import be.appify.prefab.core.versioning.EventVersion;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

/** Resolves concrete event declarations without erasing their positional type arguments. */
public final class EventBindings {
    private EventBindings() {}

    /**
     * Expands an event declaration into its validated concrete bindings.
     *
     * @param element event declaration
     * @param context processing context
     * @return concrete manifests, or an empty stream for invalid bindings
     */
    public static Stream<TypeManifest> typesOf(TypeElement element, PrefabContext context) {
        if (element.getTypeParameters().isEmpty()) {
            return Stream.of(TypeManifest.of(element.asType(), context.processingEnvironment()));
        }
        if (!validate(element, context)) {
            return Stream.empty();
        }
        return Arrays.stream(element.getAnnotationsByType(Event.class))
                .map(annotation -> typeOf(element, annotation, context));
    }

    /**
     * Resolves the concrete manifest of a binding.
     *
     * @param element event declaration
     * @param annotation concrete binding
     * @param context processing context
     * @return the bound event type
     */
    public static TypeManifest typeOf(TypeElement element, Event annotation, PrefabContext context) {
        var arguments = argumentsOf(annotation, context);
        var mirror = context.processingEnvironment().getTypeUtils()
                .getDeclaredType(element, arguments.toArray(TypeMirror[]::new));
        return TypeManifest.of(mirror, context.processingEnvironment());
    }

    /**
     * Returns the annotation matching a concrete handler or event manifest.
     *
     * @param type concrete event type
     * @return its explicit annotation
     */
    public static Event annotationFor(TypeManifest type) {
        var environment = type.processingEnvironment();
        return type.inheritedAnnotationsOfType(Event.class).stream()
                .filter(annotation -> {
                    if (type.asElement().getTypeParameters().isEmpty()) {
                        return true;
                    }
                    var arguments = argumentsOf(annotation, environment);
                    return arguments.stream().map(argument -> TypeManifest.of(argument, environment)).toList()
                            .equals(type.parameters());
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No @Event binding declared for " + type));
    }

    /**
     * Supplies a deterministic Java identifier suffix for a concrete binding.
     *
     * @param type concrete type
     * @return empty for non-generic types, otherwise a digest of its qualified type
     */
    public static String suffix(TypeManifest type) {
        if (type.parameters().isEmpty()) {
            return "";
        }
        try {
            return "_" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(type.asTypeName().toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /**
     * Emits a class literal or captured concrete token without erasing generic arguments.
     *
     * @param type event Java type
     * @return class or token expression
     */
    public static CodeBlock typeReference(TypeName type) {
        return type instanceof ParameterizedTypeName
                ? CodeBlock.of("new $T<$T>() {}", EventType.class, type)
                : CodeBlock.of("$T.class", type);
    }

    /**
     * Emits a broker-safe subscription identity for a generic binding and resolved topic.
     *
     * @param owner handler owner
     * @param type concrete event binding
     * @param topic resolved topic expression
     * @return subscription name expression
     */
    public static CodeBlock subscriptionName(TypeManifest owner, TypeManifest type, CodeBlock topic) {
        return CodeBlock.of("$T.name($S, $L.id(), $L)", EventSubscriptionNames.class,
                owner.asTypeName().toString(), typeReference(type.asTypeName()), topic);
    }

    static List<? extends TypeMirror> argumentsOf(Event annotation, PrefabContext context) {
        return argumentsOf(annotation, context.processingEnvironment());
    }

    private static List<? extends TypeMirror> argumentsOf(
            Event annotation, ProcessingEnvironment environment) {
        try {
            return Arrays.stream(annotation.typeArguments())
                    .map(type -> environment.getElementUtils().getTypeElement(type.getCanonicalName()).asType())
                    .toList();
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors();
        }
    }

    /**
     * Validates positional arguments, generic bounds and duplicate bindings.
     *
     * @param element event declaration
     * @param context processing context
     * @return whether the declaration is valid
     */
    public static boolean validate(TypeElement element, PrefabContext context) {
        var valid = true;
        var seen = new HashSet<String>();
        var parameters = element.getTypeParameters();
        for (var annotation : element.getAnnotationsByType(Event.class)) {
            if (!parameters.isEmpty()) {
                if (annotation.serialization() == Event.Serialization.AVRO) {
                    context.logError("Generic Avro event bindings are not implemented; use JSON serialization.", element);
                    valid = false;
                }
                try {
                    var version = EventVersion.parse(annotation.version());
                    if (!annotation.minSupportedVersion().isBlank()
                            && EventVersion.parse(annotation.minSupportedVersion()).compareTo(version) > 0) {
                        context.logError("@Event minSupportedVersion must not exceed its binding's version.", element);
                        valid = false;
                    }
                } catch (IllegalArgumentException e) {
                    context.logError("Invalid @Event binding version: " + e.getMessage(), element);
                    valid = false;
                }
            }
            var arguments = argumentsOf(annotation, context);
            if (arguments.size() != parameters.size()) {
                context.logError("@Event typeArguments on %s must provide exactly %d concrete arguments."
                        .formatted(element.getSimpleName(), parameters.size()), element);
                valid = false;
                continue;
            }
            if (!seen.add(arguments.toString())) {
                context.logError("Duplicate @Event binding for " + element.getSimpleName() + arguments, element);
                valid = false;
            }
            if (arguments.stream().anyMatch(argument -> !(argument instanceof DeclaredType declared)
                    || !((TypeElement) declared.asElement()).getTypeParameters().isEmpty())) {
                context.logError("@Event typeArguments must be concrete non-generic classes (no raw generic arguments).",
                        element);
                valid = false;
                continue;
            }
            var types = context.processingEnvironment().getTypeUtils();
            var substitutions = new HashMap<Element, TypeMirror>();
            for (var i = 0; i < parameters.size(); i++) {
                substitutions.put(parameters.get(i), arguments.get(i));
            }
            for (var i = 0; i < parameters.size(); i++) {
                for (var bound : parameters.get(i).getBounds()) {
                    if (!types.isAssignable(arguments.get(i), substitute(bound, substitutions, types))) {
                        context.logError("@Event type argument %s violates bound %s of %s."
                                .formatted(arguments.get(i), bound, parameters.get(i)), element);
                        valid = false;
                    }
                }
            }
        }
        return valid;
    }

    /**
     * Validates that a generic handler names one explicitly declared concrete binding.
     *
     * @param handler handler method
     * @param context processing context
     * @return whether its generic event parameter is supported
     */
    public static boolean validateHandler(ExecutableElement handler, PrefabContext context) {
        if (handler.getParameters().size() != 1
                || !(handler.getParameters().getFirst().asType() instanceof DeclaredType declared)
                || !(declared.asElement() instanceof TypeElement element)
                || element.getTypeParameters().isEmpty()
                || element.getAnnotationsByType(Event.class).length == 0) {
            return true;
        }
        var types = context.processingEnvironment().getTypeUtils();
        var matches = Arrays.stream(element.getAnnotationsByType(Event.class))
                .map(annotation -> argumentsOf(annotation, context))
                .anyMatch(arguments -> {
                    if (arguments.size() != declared.getTypeArguments().size()) {
                        return false;
                    }
                    for (var i = 0; i < arguments.size(); i++) {
                        if (!types.isSameType(arguments.get(i), declared.getTypeArguments().get(i))) {
                            return false;
                        }
                    }
                    return true;
                });
        if (!matches) {
            context.logError("@EventHandler must use a declared concrete @Event binding; raw, wildcard and unresolved "
                    + "generic event arguments are not supported: " + declared, handler);
        }
        return matches;
    }

    private static TypeMirror substitute(TypeMirror bound,
            Map<Element, TypeMirror> substitutions, Types types) {
        if (bound instanceof TypeVariable variable) {
            return substitutions.getOrDefault(variable.asElement(), bound);
        }
        if (bound instanceof WildcardType wildcard) {
            return types.getWildcardType(
                    wildcard.getExtendsBound() == null ? null : substitute(wildcard.getExtendsBound(), substitutions, types),
                    wildcard.getSuperBound() == null ? null : substitute(wildcard.getSuperBound(), substitutions, types));
        }
        if (bound instanceof DeclaredType declared) {
            var arguments = declared.getTypeArguments().stream()
                    .map(argument -> substitute(argument, substitutions, types)).toArray(TypeMirror[]::new);
            return types.getDeclaredType((TypeElement) declared.asElement(), arguments);
        }
        return bound;
    }
}
