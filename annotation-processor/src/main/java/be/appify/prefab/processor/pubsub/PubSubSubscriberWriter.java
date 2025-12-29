package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.processor.CaseUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.lang.model.element.ExecutableElement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

public class PubSubSubscriberWriter {

    public void writePubSubSubscriber(
            TypeManifest owner,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.pubsub");

        var name = "%sPubSubSubscriber".formatted(owner.simpleName());
        var packageName = new TypeManifest(eventHandlers.getFirst().getEnclosingElement().asType(),
                context.processingEnvironment()).packageName();
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(packageName + ".infrastructure.pubsub", name))
                        .build())
                .addField(FieldSpec.builder(Executor.class, "executor", PRIVATE, FINAL)
                        .initializer("$T.newSingleThreadExecutor()", ClassName.get(Executors.class))
                        .build());

        var fields = addFields(eventHandlers, context, type);
        addEventHandlers(eventHandlers, context, type);
        var topics = eventHandlers.stream()
                .map(e -> rootEventType(e, context).annotationsOfType(Event.class).stream().findFirst().orElseThrow()
                        .topic())
                .collect(Collectors.toSet());
        type.addMethod(constructor(topics, owner, fields, eventHandlers, context));
        fileWriter.writeFile(packageName, name, type.build());
    }

    private static Set<FieldSpec> addFields(
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
                        "Cannot write PubSub consumer for %s, it is neither an Aggregate nor a Component".formatted(
                                target.simpleName()),
                        eventHandler);
            }
        }
        return fields;
    }

    private void addEventHandlers(
            List<ExecutableElement> allEventHandlers,
            PrefabContext context,
            TypeSpec.Builder type
    ) {
        var eventHandlersByEventType = allEventHandlers.stream().collect(groupingBy(e -> rootEventType(e, context)));
        for (Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent : eventHandlersByEventType.entrySet()) {
            var eventType = eventHandlersForEvent.getKey();
            var method = MethodSpec.methodBuilder("on%s".formatted(eventType.simpleName().replace(".", "")))
                    .addModifiers(PRIVATE)
                    .addParameter(eventType.asTypeName(), "event")
                    .addStatement("log.debug($S, event)", "Received event {}");
            var eventHandlers = eventHandlersForEvent.getValue();
            if (eventHandlers.size() == 1 && sameType(eventType, eventHandlers.getFirst(), context)) {
                singleTypeHandler(context, eventHandlers.getFirst(), method, "event");
                type.addMethod(method.build());
            } else {
                multiTypeHandler(context, eventHandlers, method);
                type.addMethod(method.build());
            }
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
        method.addCode("    default -> {}\n");
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

    private static TypeManifest rootEventType(ExecutableElement eventHandler, PrefabContext context) {
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

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().getFirst().asType(), context.processingEnvironment());
    }

    private static MethodSpec constructor(
            Set<String> topics,
            TypeManifest owner,
            Set<FieldSpec> fields,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        fields.forEach(field -> constructor.addParameter(ParameterSpec.builder(field.type(), field.name()).build()));
        constructor.addParameter(PubSubUtil.class, "pubSub");
        topics.forEach(topic -> addTopic(owner, eventHandlers, context, topic, constructor));
        fields.forEach(field -> constructor.addStatement("this.$L = $L", field.name(), field.name()));
        return constructor.build();
    }

    private static void addTopic(TypeManifest owner, List<ExecutableElement> eventHandlers, PrefabContext context,
            String topic, MethodSpec.Builder constructor) {
        var eventType = eventTypeOf(eventHandlers, context, topic);
        var topicVariableName = uncapitalize(eventType.simpleName().replace(".", "")) + "Topic";
        var eventName = eventType.simpleName().replace(".", "");
        if (topic.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, topicVariableName)
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("pubSub.subscribe($L, $S, $T.class, this::on$L, executor)",
                            topicVariableName,
                            CaseUtil.toKebabCase(owner.simpleName()) + "-on-" + CaseUtil.toKebabCase(
                                    eventName),
                            eventType.asTypeName(),
                            eventName);
        } else {
            constructor.addStatement("pubSub.subscribe($S, $S, $T.class, this::on$L, executor)",
                    topic,
                    CaseUtil.toKebabCase(owner.simpleName()) + "-on-" + CaseUtil.toKebabCase(
                            eventName),
                    eventType.asTypeName(),
                    eventName);
        }
    }

    private static TypeManifest eventTypeOf(List<ExecutableElement> eventHandlers, PrefabContext context,
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
}
