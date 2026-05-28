package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.CaseUtil;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.ConsumerWriterSupport;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;

import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static be.appify.prefab.core.annotations.EventHandlerConfig.Util.hasConsumeFromTopicsFilter;
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
            List<ExecutableElement> eventHandlers
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

        validateTopicAncestors(resolvedHandlers);

        var fileWriter = new OutputTargetFileOutput(context, "infrastructure.kafka", OutputTarget.MAIN);

        var name = "%sKafkaConsumer".formatted(owner.simpleName());
        var packageName = TypeManifest.of(resolvedHandlers.getFirst().getEnclosingElement().asType(),
                context.processingEnvironment()).packageName();
        var isAsyncCommit = !owner.annotationsOfType(AsyncCommit.class).isEmpty();
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, Modifier.STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(packageName + ".infrastructure.kafka", name))
                        .build());

        var fields = support.addFields(resolvedHandlers, context, type);
        addEventHandlers(resolvedHandlers, owner, type, isAsyncCommit);
        type.addMethod(constructor(fields));
        fileWriter.writeFile(packageName, name, type.build());
    }

    private void validateTopicAncestors(List<ExecutableElement> eventHandlers) {
        var topics = eventHandlers.stream()
                .map(e -> listenerEventTypeFor(support.rootEventType(e, context))
                        .annotationsOfType(Event.class).stream().findFirst().orElseThrow().topic())
                .flatMap(java.util.Arrays::stream)
                .collect(Collectors.toSet());
        topics.forEach(topic -> support.eventTypeOf(eventHandlers, context, topic));
    }

    private void addEventHandlers(
            List<ExecutableElement> allEventHandlers,
            TypeManifest owner,
            TypeSpec.Builder type,
            boolean asyncCommit
    ) {
        var eventHandlersByEventType = allEventHandlers.stream()
                .collect(Collectors.groupingBy(
                        e -> listenerEventTypeFor(support.rootEventType(e, context)),
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
            if (asyncCommit) {
                method.addAnnotation(Transactional.class);
            }
            support.writeEventHandler(context, type, eventHandlersForEvent, listenerParamType, method);
        }
    }

    private TypeManifest listenerEventTypeFor(TypeManifest rootType) {
        return avscInterfaceOf(rootType).orElse(rootType);
    }

    private Optional<TypeManifest> avscInterfaceOf(TypeManifest type) {
        if (type.asElement() == null || !isAvscGeneratedRecord(type.asElement())) {
            return Optional.empty();
        }
        return (type.asElement()).getInterfaces().stream()
                .filter(iface -> iface.getKind().name().equals("DECLARED"))
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Avsc.class) != null)
                .findFirst()
                .map(iface -> TypeManifest.of(iface.asType(), context.processingEnvironment()));
    }

    private AnnotationSpec kafkaListener(TypeManifest owner, Event event, String eventName) {
        var kafkaListener = AnnotationSpec.builder(KafkaListener.class);
        var eventHandlerConfig = owner.inheritedAnnotationsOfType(EventHandlerConfig.class).stream().findFirst();
        var topicsToSubscribe = effectiveTopics(owner, event, eventHandlerConfig.orElse(null));
        for (var topic : topicsToSubscribe) {
            kafkaListener.addMember("topics", "$S", topic);
        }
        kafkaListener.addMember("groupId", "$S",
                        "${spring.application.name}." + CaseUtil.toKebabCase(owner.simpleName())
                                + "-on-" + CaseUtil.toKebabCase(eventName))
                .addMember("concurrency", "$S", concurrencyExpression(owner));
        var customConfig = eventHandlerConfig
                .map(EventHandlerConfig.Util::hasCustomConfig)
                .orElse(false);
        if (customConfig) {
            kafkaListener.addMember("errorHandler", "$S", "%sKafkaErrorHandler".formatted(uncapitalize(owner.simpleName())));
        }
        eventHandlerConfig
                .filter(EventHandlerConfig.Util::hasCustomAutoOffsetReset)
                .ifPresent(config -> kafkaListener.addMember(
                        "properties",
                        "$S",
                        "auto.offset.reset=" + config.autoOffsetReset()));
        return kafkaListener.build();
    }

    private List<String> effectiveTopics(TypeManifest owner, Event event, EventHandlerConfig config) {
        var topics = List.of(event.topic());
        if (hasConsumeFromTopicsFilter(config)) {
            var consumeFromTopics = List.of(config.consumeFromTopics());
            if (!new HashSet<>(topics).containsAll(consumeFromTopics)) {
                context.logError("Event %s specifies topics %s but EventHandlerConfig specifies consumeFromTopics %s. All consumeFromTopics must be included in the event's topics."
                        .formatted(owner, topics, consumeFromTopics), owner.asElement());
                return topics;
            }
            return consumeFromTopics;
        }
        return topics;
    }

    private MethodSpec constructor(Set<FieldSpec> fields) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        fields.forEach(field -> constructor.addParameter(ParameterSpec.builder(field.type(), field.name()).build()));
        fields.forEach(field -> constructor.addStatement("this.$L = $L", field.name(), field.name()));
        return constructor.build();
    }
}
