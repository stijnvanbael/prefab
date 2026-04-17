package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import be.appify.prefab.processor.CaseUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.ConsumerWriterSupport;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.ConsumerWriterSupport.concurrencyExpression;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isAvscGeneratedRecord;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class KafkaConsumerWriter {
    private static final ConsumerWriterSupport support = new ConsumerWriterSupport(Event.Platform.KAFKA);
    private final PrefabContext context;

    public KafkaConsumerWriter(PrefabContext context) {
        this.context = context;
    }

    void writeKafkaConsumer(
            TypeManifest owner,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var resolvedHandlers = eventHandlers.stream()
                .filter(h -> !TypeManifest.containsUnresolvedType(h.getParameters().getFirst().asType()))
                .toList();

        if (resolvedHandlers.isEmpty()) {
            eventHandlers.forEach(context::deferEventHandler);
            return;
        }

        if (support.hasAvscEventWithoutConcreteType(resolvedHandlers, context)) {
            resolvedHandlers.forEach(context::deferEventHandler);
            return;
        }

        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var name = "%sKafkaConsumer".formatted(owner.simpleName());
        var packageName = TypeManifest.of(resolvedHandlers.getFirst().getEnclosingElement().asType(),
                context.processingEnvironment()).packageName();
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, Modifier.STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(packageName + ".infrastructure.kafka", name))
                        .build());

        var fields = support.addFields(resolvedHandlers, context, type);
        addEventHandlers(resolvedHandlers, owner, context, type);
        var topics = resolvedHandlers.stream()
                .map(e -> listenerEventTypeFor(support.rootEventType(e, context), context)
                        .annotationsOfType(Event.class).stream().findFirst()
                        .orElseThrow()
                        .topic())
                .collect(Collectors.toSet());
        type.addMethod(constructor(topics, fields, resolvedHandlers, context));
        fileWriter.writeFile(packageName, name, type.build());
    }

    private void addEventHandlers(
            List<ExecutableElement> allEventHandlers,
            TypeManifest owner,
            PrefabContext context,
            TypeSpec.Builder type
    ) {
        var eventHandlersByEventType = allEventHandlers.stream()
                .collect(Collectors.groupingBy(
                        e -> listenerEventTypeFor(support.rootEventType(e, context), context),
                        LinkedHashMap::new,
                        Collectors.toList()));
        for (Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent : eventHandlersByEventType.entrySet()) {
            var eventType = eventHandlersForEvent.getKey();
            var concreteTypes = support.concreteEventTypes(eventType, context);
            var listenerParamType = concreteTypes.size() == 1 ? concreteTypes.getFirst() : eventType;
            var annotation = eventType.annotationsOfType(Event.class).stream().findFirst().orElseThrow();
            var eventName = eventType.simpleName().replace(".", "");
            var method = MethodSpec.methodBuilder("on%s".formatted(eventName))
                    .addModifiers(PUBLIC)
                    .addAnnotation(kafkaListener(owner, annotation, eventName))
                    .addParameter(listenerParamType.asTypeName(), "event")
                    .addStatement("log.debug($S, event)", "Received event {}");
            support.writeEventHandler(context, type, eventHandlersForEvent, eventType, method);
        }
    }

    private TypeManifest listenerEventTypeFor(TypeManifest rootType, PrefabContext context) {
        return avscInterfaceOf(rootType, context).orElse(rootType);
    }

    private Optional<TypeManifest> avscInterfaceOf(TypeManifest type, PrefabContext context) {
        if (type.asElement() == null || !isAvscGeneratedRecord(type.asElement())) {
            return Optional.empty();
        }
        return ((TypeElement) type.asElement()).getInterfaces().stream()
                .filter(iface -> iface.getKind().name().equals("DECLARED"))
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Avsc.class) != null)
                .findFirst()
                .map(iface -> TypeManifest.of(iface.asType(), context.processingEnvironment()));
    }

    private static AnnotationSpec kafkaListener(TypeManifest owner, Event event, String eventName) {
        var kafkaListener = AnnotationSpec.builder(KafkaListener.class)
                .addMember("topics", "$S", event.topic())
                .addMember("groupId", "$S",
                        "${spring.application.name}." + CaseUtil.toKebabCase(owner.simpleName())
                                + "-on-" + CaseUtil.toKebabCase(eventName))
                .addMember("concurrency", "$S", concurrencyExpression(owner));
        var customConfig = owner.inheritedAnnotationsOfType(EventHandlerConfig.class).stream().findFirst()
                .map(EventHandlerConfig.Util::hasCustomConfig)
                .orElse(false);
        if (customConfig) {
            kafkaListener.addMember("errorHandler", "$S", "%sKafkaErrorHandler".formatted(uncapitalize(owner.simpleName())));
        }
        return kafkaListener.build();
    }

    private static MethodSpec constructor(
            Set<String> topics,
            Set<FieldSpec> fields,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        fields.forEach(field -> constructor.addParameter(ParameterSpec.builder(field.type(), field.name()).build()));
        constructor.addParameter(KafkaJsonTypeResolver.class, "typeResolver");
        topics.forEach(topic -> addTopic(eventHandlers, context, topic, constructor));
        fields.forEach(field -> constructor.addStatement("this.$L = $L", field.name(), field.name()));
        return constructor.build();
    }

    private static void addTopic(List<ExecutableElement> eventHandlers, PrefabContext context, String topic,
            MethodSpec.Builder constructor) {
        var concreteTypes = concreteTypesForTopic(eventHandlers, context, topic);
        if (concreteTypes.isEmpty()) {
            support.eventTypeOf(eventHandlers, context, topic);
            return;
        }
        var registrationTypes = resolveRegistrationTypes(concreteTypes, eventHandlers, context, topic);
        if (topic.matches("\\$\\{.+}")) {
            var eventType = support.eventTypeOf(eventHandlers, context, topic);
            var topicVariableName = uncapitalize(eventType.simpleName().replace(".", "")) + "Topic";
            constructor.addParameter(ParameterSpec.builder(String.class, topicVariableName)
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", topic)
                            .build())
                    .build());
            registrationTypes.forEach(type ->
                    constructor.addStatement("typeResolver.registerType($L, $T.class)", topicVariableName, type.asTypeName()));
        } else {
            registrationTypes.forEach(type ->
                    constructor.addStatement("typeResolver.registerType($S, $T.class)", topic, type.asTypeName()));
        }
    }

    private static List<TypeManifest> resolveRegistrationTypes(
            List<TypeManifest> concreteTypes,
            List<ExecutableElement> eventHandlers,
            PrefabContext context,
            String topic) {
        if (concreteTypes.size() <= 1) {
            return concreteTypes;
        }
        return sharedEventInterfaceOf(concreteTypes, topic, context)
                .map(List::of)
                .orElseGet(() -> concreteTypes.stream()
                        .sorted(Comparator.comparing(t -> t.simpleName()))
                        .toList());
    }

    private static Optional<TypeManifest> sharedEventInterfaceOf(
            List<TypeManifest> concreteTypes,
            String topic,
            PrefabContext context) {
        return concreteTypes.stream()
                .filter(t -> t.asElement() != null)
                .flatMap(t -> ((TypeElement) t.asElement()).getInterfaces().stream())
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Event.class) != null
                        && iface.getAnnotation(Event.class).topic().equals(topic))
                .distinct()
                .findFirst()
                .map(iface -> TypeManifest.of(iface.asType(), context.processingEnvironment()));
    }

    private static List<TypeManifest> concreteTypesForTopic(List<ExecutableElement> eventHandlers, PrefabContext context,
            String topic) {
        var rootTypes = eventHandlers.stream()
                .map(h -> support.rootEventType(h, context))
                .filter(t -> t.annotationsOfType(Event.class).stream().anyMatch(e -> e.topic().equals(topic)))
                .distinct()
                .toList();
        if (rootTypes.size() <= 1) {
            return rootTypes.stream()
                    .flatMap(t -> support.concreteEventTypes(t, context).stream())
                    .toList();
        }
        var allAvscGenerated = rootTypes.stream()
                .allMatch(t -> t.asElement() != null && isAvscGeneratedRecord(t.asElement()));
        if (!allAvscGenerated) {
            return List.of();
        }
        return rootTypes.stream()
                .flatMap(t -> support.concreteEventTypes(t, context).stream())
                .distinct()
                .toList();
    }
}
