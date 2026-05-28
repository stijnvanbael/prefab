package be.appify.prefab.processor.rest.stream;

import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static be.appify.prefab.processor.rest.stream.SseRegistryWriter.sseRegistrySimpleName;
import static org.apache.commons.text.WordUtils.uncapitalize;

/**
 * Writes service methods for the {@code @Streaming} push model.
 *
 * <p>For each {@code @EventHandler @ByReference @Stream} method, this writer generates a service
 * method that applies the event handler and persists the result, then pushes the event payload
 * to any connected SSE client via the generated {@code {Aggregate}SseRegistry}.
 */
class StreamServiceWriter {

    private final PrefabContext context;

    StreamServiceWriter(PrefabContext context) {
        this.context = context;
    }

    /**
     * Adds augmented event-handler service methods for all push-model {@code @Streaming} methods.
     *
     * @param manifest the aggregate manifest
     * @param builder  the service TypeSpec builder
     */
    void writePushServiceMethods(ClassManifest manifest, TypeSpec.Builder builder) {
        pushStreamMethods(manifest).forEach(method -> builder.addMethod(buildPushServiceMethod(manifest, method)));
    }

    /**
     * Returns all push-model stream methods on the aggregate (annotated with {@code @EventHandler},
     * {@code @ByReference}, and {@code @Streaming}).
     *
     * @param manifest the aggregate manifest
     * @return list of matching executable elements
     */
    List<ExecutableElement> pushStreamMethods(ClassManifest manifest) {
        return manifest.type().asElement().getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .map(ExecutableElement.class::cast)
                .filter(e -> e.getAnnotation(EventHandler.class) != null)
                .filter(e -> e.getAnnotation(ByReference.class) != null)
                .filter(e -> e.getAnnotation(Streaming.class) != null)
                .toList();
    }

    private MethodSpec buildPushServiceMethod(ClassManifest manifest, ExecutableElement method) {
        var methodName = method.getSimpleName().toString();
        var byReference = method.getAnnotation(ByReference.class);
        var stream = method.getAnnotation(Streaming.class);
        var eventType = TypeManifest.of(
                method.getParameters().getFirst().asType(), context.processingEnvironment());

        validateTerminalField(stream, eventType, method);

        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var registryFieldName = uncapitalize(sseRegistrySimpleName(manifest));

        var isVoidReturn = method.getReturnType().getKind() == TypeKind.VOID;
        var referenceFieldType = getFieldType(eventType, byReference.property());
        var valueAccessor = referenceFieldType != null && referenceFieldType.isSingleValueType()
                ? "." + referenceFieldType.singleValueAccessor() + "()"
                : "";

        var mapBlock = isVoidReturn
                ? CodeBlock.of(
                        """
                        .map(aggregate -> {
                            aggregate.$L(event);
                            return $L.save(aggregate);
                        })
                        """, methodName, repositoryName)
                : CodeBlock.of(
                        """
                        .map(aggregate -> {
                            var updated = aggregate.$L(event);
                            return $L.save(updated);
                        })
                        """, methodName, repositoryName);

        var dataField = resolveDataField(stream, eventType);
        var dataExpression = dataField.map("event.%s()"::formatted).orElse("event");
        var terminalClause = buildTerminalClause(stream);

        var ssePushBlock = CodeBlock.of(
                """
                $L.findById(event.$L()$L).ifPresent(emitter -> {
                    try {
                        emitter.send($T.event().name($S).data($L));
                        $L
                    } catch ($T e) {
                        emitter.completeWithError(e);
                    }
                })""",
                registryFieldName, byReference.property(), valueAccessor,
                SseEmitter.class, stream.event(), dataExpression,
                terminalClause,
                IOException.class);

        var methodSpec = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC);

        if (eventType.inheritedAnnotationsOfType(be.appify.prefab.core.annotations.Event.class).isEmpty()) {
            methodSpec.addAnnotation(EventListener.class);
        }

        methodSpec.addParameter(eventType.asTypeName(), "event")
                .addStatement(CodeBlock.builder()
                        .add("$L.findById(event.$L()$L)",
                                repositoryName, byReference.property(), valueAccessor)
                        .add(mapBlock)
                        .add(".orElseThrow()")
                        .build())
                .addStatement(ssePushBlock);

        return methodSpec.build();
    }

    private String buildTerminalClause(Streaming stream) {
        if (stream.terminal().isEmpty()) {
            return "";
        }
        return "if (event.%s()) { emitter.complete(); }".formatted(stream.terminal());
    }

    private Optional<String> resolveDataField(Streaming stream, TypeManifest eventType) {
        var eventName = stream.event();
        if (eventName.isEmpty() || eventType.asElement() == null) {
            return Optional.empty();
        }
        var hasMatchingField = eventType.asElement().getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .anyMatch(f -> f.getSimpleName().toString().equals(eventName));
        return hasMatchingField ? Optional.of(eventName) : Optional.empty();
    }

    private void validateTerminalField(Streaming stream, TypeManifest eventType, ExecutableElement method) {
        if (stream.terminal().isEmpty() || eventType.asElement() == null) {
            return;
        }
        var terminalField = eventType.asElement().getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .filter(f -> f.getSimpleName().toString().equals(stream.terminal()))
                .findFirst();

        if (terminalField.isEmpty()) {
            context.logError(
                    "@Streaming terminal = \"%s\" does not exist as a field on %s".formatted(
                            stream.terminal(), eventType.simpleName()),
                    method);
            return;
        }

        var fieldTypeMirror = terminalField.get().asType();
        var isBooleanField = fieldTypeMirror.getKind() == javax.lang.model.type.TypeKind.BOOLEAN
                || "java.lang.Boolean".equals(fieldTypeMirror.toString());
        if (!isBooleanField) {
            context.logError(
                    "@Streaming terminal = \"%s\" on %s must be a boolean or Boolean field".formatted(
                            stream.terminal(), eventType.simpleName()),
                    method);
        }
    }

    private TypeManifest getFieldType(TypeManifest eventType, String fieldName) {
        if (eventType.asElement() == null) {
            return null;
        }
        return eventType.asElement().getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .filter(f -> f.getSimpleName().toString().equals(fieldName))
                .map(f -> TypeManifest.of(f.asType(), context.processingEnvironment()))
                .findFirst()
                .orElse(null);
    }
}

