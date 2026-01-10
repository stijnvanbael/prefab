package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.pubsub.PubSubUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;
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

class PubSubSubscriberWriter {
    private static final ConsumerWriterSupport support = new ConsumerWriterSupport(Event.Platform.PUB_SUB);

    void writePubSubSubscriber(
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

        var fields = support.addFields(eventHandlers, context, type);
        addEventHandlers(eventHandlers, context, type);
        var topics = eventHandlers.stream()
                .map(e -> support.rootEventType(e, context).annotationsOfType(Event.class).stream().findFirst()
                        .orElseThrow()
                        .topic())
                .collect(Collectors.toSet());
        type.addMethod(constructor(topics, owner, fields, eventHandlers, context));
        fileWriter.writeFile(packageName, name, type.build());
    }

    private void addEventHandlers(
            List<ExecutableElement> allEventHandlers,
            PrefabContext context,
            TypeSpec.Builder type
    ) {
        var eventHandlersByEventType = allEventHandlers.stream()
                .collect(groupingBy(e -> support.rootEventType(e, context)));
        for (Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent : eventHandlersByEventType.entrySet()) {
            var eventType = eventHandlersForEvent.getKey();
            var method = MethodSpec.methodBuilder("on%s".formatted(eventType.simpleName().replace(".", "")))
                    .addModifiers(PRIVATE)
                    .addParameter(eventType.asTypeName(), "event")
                    .addStatement("log.debug($S, event)", "Received event {}");
            support.writeEventHandler(context, type, eventHandlersForEvent, eventType, method);
        }
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
        var eventType = support.eventTypeOf(eventHandlers, context, topic);
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
}
