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
import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

public class PubSubSubscriberWriter {

    public void writePubSubSubscriber(
            String topic,
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
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, Modifier.STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(packageName + ".infrastructure.pubsub", name))
                        .build());

        var fields = addFields(eventHandlers, context, type);
        addEventHandlers(eventHandlers, context, type);
        type.addMethod(constructor(topic, owner, fields, eventHandlers, context));
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
        var eventHandlersByEventType = allEventHandlers.stream().collect(groupingBy(e -> eventType(e, context)));
        for (Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent : eventHandlersByEventType.entrySet()) {
            var event = eventHandlersForEvent.getKey();
            var method = MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                    .addModifiers(PUBLIC)
                    .addParameter(event.asTypeName(), "event")
                    .addStatement("log.debug($S, event)", "Received event {}");
            var eventHandlers = eventHandlersForEvent.getValue();
            if (eventHandlers.size() == 1) {
                singleTypeHandler(context, eventHandlers.getFirst(), method, event, "event");
                type.addMethod(method.build());
            } else {
                multiTypeHandler(context, eventHandlers, method, event);
                type.addMethod(method.build());
            }
        }
    }

    private void multiTypeHandler(
            PrefabContext context,
            List<ExecutableElement> eventHandlers,
            MethodSpec.Builder method,
            TypeManifest event
    ) {
        method.addCode("switch (event) {\n");
        for (ExecutableElement eventHandler : eventHandlers) {
            var parameter = eventHandler.getParameters().getFirst();
            var type = new TypeManifest(parameter.asType(), context.processingEnvironment());
            method.addCode("    case $T e -> ", type.asTypeName());
            singleTypeHandler(context, eventHandler, method, event, "e");
        }
        method.addCode("}");
    }

    private void singleTypeHandler(
            PrefabContext context,
            ExecutableElement eventHandler,
            MethodSpec.Builder method,
            TypeManifest event,
            String variableName
    ) {
        var target = new TypeManifest(eventHandler.getEnclosingElement().asType(),
                context.processingEnvironment());
        if (!target.annotationsOfType(Aggregate.class).isEmpty()) {
            method.addStatement("$NService.on$L($L)",
                    uncapitalize(target.simpleName()), event.simpleName(), variableName);
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
        var type = new TypeManifest(eventHandler.getParameters().getFirst().asType(), context.processingEnvironment());
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

    private static MethodSpec constructor(
            String topic,
            TypeManifest owner,
            Set<FieldSpec> fields,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        fields.forEach(field -> constructor.addParameter(ParameterSpec.builder(field.type(), field.name()).build()));
        constructor
                .addParameter(PubSubUtil.class, "pubSub")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", topic)
                                .build())
                        .build());
        fields.forEach(field -> constructor.addStatement("this.$L = $L", field.name(), field.name()));
        var eventType = eventTypeOf(eventHandlers, context, topic);
        constructor.addStatement("pubSub.subscribe(topic, $S, $T.class, this::on$L)",
                CaseUtil.toKebabCase(owner.simpleName()) + "-on-" + CaseUtil.toKebabCase(eventType.simpleName()),
                eventType.asTypeName(),
                eventType.simpleName());
        return constructor.build();
    }

    private static TypeManifest eventTypeOf(List<ExecutableElement> eventHandlers, PrefabContext context, String topic) {
        var eventTypes = eventHandlers.stream()
                .map(e -> eventType(e, context))
                .collect(Collectors.toSet());
        if(eventTypes.size() > 1) {
            reportNoCommonAncestor(eventHandlers, context, topic, eventTypes);
        }
        return eventTypes.stream().findFirst().orElseThrow();
    }

    private static void reportNoCommonAncestor(List<ExecutableElement> eventHandlers, PrefabContext context, String topic,
            Set<TypeManifest> eventTypes) {
        context.logError("Events [%s] share the same topic [%s] but have no common ancestor. Make sure they extend the same supertype and there is a single @Event annotation on the supertype.".formatted(
                eventTypes.stream()
                        .map(TypeManifest::simpleName)
                        .collect(Collectors.joining(", ")),
                topic
        ), eventHandlers.getFirst());
    }
}
