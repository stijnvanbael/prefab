package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import org.springframework.stereotype.Component;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

/**
 * Support class for writing event consumer classes for different platforms (e.g., Kafka, Pub/Sub).
 */
public class ConsumerWriterSupport {
    private final Event.Platform platform;

    /**
     * Creates a new instance of ConsumerWriterSupport for the specified platform.
     *
     * @param platform
     *         event platform
     */
    public ConsumerWriterSupport(Event.Platform platform) {
        this.platform = platform;
    }

    /**
     * Adds fields for all aggregates and components that have event handlers in the given list.
     *
     * @param eventHandlers
     *         list of event handler methods
     * @param context
     *         prefab context
     * @param type
     *         type spec builder to add fields to
     * @return set of added field specs
     */
    public Set<FieldSpec> addFields(
            List<ExecutableElement> eventHandlers,
            PrefabContext context,
            TypeSpec.Builder type
    ) {
        var fields = new HashSet<FieldSpec>();
        for (ExecutableElement eventHandler : eventHandlers) {
            var target = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
            if (!target.annotationsOfType(Aggregate.class).isEmpty()) {
                var serviceClass = ClassName.get(
                        "%s.application".formatted(target.packageName()),
                        "%sService".formatted(target.simpleName())
                );
                var field = FieldSpec.builder(serviceClass, "%sService".formatted(uncapitalize(target.simpleName())),
                        PRIVATE, FINAL).build();
                if (fields.add(field)) {
                    type.addField(field);
                }
            } else if (!target.annotationsOfType(Component.class).isEmpty()) {
                var field = FieldSpec.builder(target.asTypeName(), uncapitalize(target.simpleName()), PRIVATE, FINAL)
                        .build();
                if (fields.add(field)) {
                    type.addField(field);
                }
            } else {
                context.logError(
                        "Cannot write %s consumer for %s, it is neither an Aggregate nor a Component".formatted(
                                platform, target.simpleName()),
                        eventHandler);
            }
        }
        return fields;
    }

    /**
     * Writes an event handler method for the given event type and its associated event handlers.
     *
     * @param context
     *         prefab context
     * @param type
     *         type spec builder to add the method to
     * @param eventHandlersForEvent
     *         map entry containing the event type and its associated event handlers
     * @param eventType
     *         the event type manifest
     * @param method
     *         method spec builder for the event handler method
     */
    public void writeEventHandler(PrefabContext context, TypeSpec.Builder type,
            Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent, TypeManifest eventType,
            MethodSpec.Builder method) {
        var eventHandlers = eventHandlersForEvent.getValue();
        if (eventHandlers.size() == 1 && sameType(eventType, eventHandlers.getFirst(), context)) {
            singleTypeHandler(context, eventHandlers.getFirst(), method, "event");
            type.addMethod(method.build());
        } else {
            multiTypeHandler(context, eventHandlers, method);
            type.addMethod(method.build());
        }
    }

    private static boolean sameType(TypeManifest eventType, ExecutableElement eventHandler, PrefabContext context) {
        var parameter = eventType(eventHandler, context);
        return Objects.equals(parameter, eventType);
    }

    private void multiTypeHandler(
            PrefabContext context,
            List<ExecutableElement> eventHandlers,
            MethodSpec.Builder method
    ) {
        method.addCode("switch (event) {\n");
        for (ExecutableElement eventHandler : eventHandlers) {
            var parameter = eventHandler.getParameters().getFirst();
            var type = new TypeManifest(parameter.asType(), context.processingEnvironment());
            method.addCode("    case $T e -> ", type.asTypeName());
            singleTypeHandler(context, eventHandler, method, "e");
        }
        method.addCode("""
                    default -> {
                    }
                """);
        method.addCode("}");
    }

    private void singleTypeHandler(
            PrefabContext context,
            ExecutableElement eventHandler,
            MethodSpec.Builder method,
            String variableName
    ) {
        var target = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
        if (!target.annotationsOfType(Aggregate.class).isEmpty()) {
            method.addStatement("$NService.$L($L)",
                    uncapitalize(target.simpleName()), eventHandler.getSimpleName(), variableName);
        } else if (!target.annotationsOfType(Component.class).isEmpty()) {
            method.addStatement("$N.$L($L)",
                    uncapitalize(target.simpleName()), eventHandler.getSimpleName(), variableName);
        } else {
            context.logError(
                    "Cannot write Kafka consumer for %s, it is neither an Aggregate nor a Component".formatted(
                            target.simpleName()),
                    eventHandler);
        }
    }

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().getFirst().asType(), context.processingEnvironment());
    }

    /**
     * Determines the common event type for the given event handlers based on the specified topic.
     *
     * @param eventHandlers
     *         list of event handler methods
     * @param context
     *         prefab context
     * @param topic
     *         event topic
     * @return common event type manifest
     */
    public TypeManifest eventTypeOf(List<ExecutableElement> eventHandlers, PrefabContext context,
            String topic) {
        var eventTypes = eventHandlers.stream()
                .map(e -> rootEventType(e, context))
                .filter(type -> type.annotationsOfType(Event.class).stream()
                        .anyMatch(event -> event.topic().equals(topic)))
                .collect(Collectors.toSet());
        if (eventTypes.size() > 1) {
            reportNoCommonAncestor(eventHandlers, context, topic, eventTypes);
        }
        return eventTypes.stream().findFirst().orElseThrow();
    }

    /**
     * Retrieves the root event type for the given event handler method.
     *
     * @param eventHandler
     *         event handler method
     * @param context
     *         prefab context
     * @return root event type manifest
     */
    public TypeManifest rootEventType(ExecutableElement eventHandler, PrefabContext context) {
        var type = eventType(eventHandler, context);
        if (type.annotationsOfType(Event.class).isEmpty()) {
            return type.supertypeWithAnnotation(Event.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Event parameter type %s or one of its supertypes of method %s is not annotated with @Event".formatted(
                                    type.simpleName(),
                                    eventHandler.getSimpleName()
                            )));
        }
        return type;
    }

    private static void reportNoCommonAncestor(List<ExecutableElement> eventHandlers, PrefabContext context,
            String topic,
            Set<TypeManifest> eventTypes) {
        context.logError(
                "Events [%s] share the same topic [%s] but have no common ancestor. Make sure they extend the same supertype and there is a single @Event annotation on the supertype.".formatted(
                        eventTypes.stream()
                                .map(TypeManifest::simpleName)
                                .collect(Collectors.joining(", ")),
                        topic
                ), eventHandlers.getFirst());
    }

    /**
     * Retrieves the concurrency expression from the EventHandlerConfig annotation of the given owner type.
     *
     * @param owner
     *         owner type manifest
     * @return concurrency expression
     */
    public static String concurrencyExpression(TypeManifest owner) {
        return owner.annotationsOfType(EventHandlerConfig.class)
                .stream()
                .findFirst()
                .map(EventHandlerConfig::concurrency)
                .orElse("1");
    }

    /**
     * Generates a code block to access the partitioning key field of the given event type.
     *
     * @param event
     *         event type manifest
     * @param context
     *         prefab context
     * @return code block for accessing the partitioning key field
     */
    public static Optional<CodeBlock> keyField(TypeManifest event, PrefabContext context) {
        return event.methodsWith(PartitioningKey.class).stream()
                .findFirst()
                .map(method -> {
                    if (new TypeManifest(method.getReturnType(), context.processingEnvironment()).is(Reference.class)) {
                        return CodeBlock.of("event.$L().id()", method.getSimpleName().toString());
                    } else {
                        return CodeBlock.of("event.$L()", method.getSimpleName().toString());
                    }
                })
                .or(() -> {
                    context.logNote(("No partitioning key found on event %s. Annotate a field with @PartitioningKey if you need " +
                            "guaranteed ordering on all events with the same value for that field.")
                            .formatted(event.simpleName()), event.asElement());
                    return Optional.empty();
                });
    }
}
