package be.appify.prefab.processor.event.kafka;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.ConsumerWriterSupport.concurrencyExpression;
import static java.util.stream.Collectors.groupingBy;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class KafkaConsumerWriter {
    private static final ConsumerWriterSupport support = new ConsumerWriterSupport(Event.Platform.KAFKA);

    void writeKafkaConsumer(
            TypeManifest owner,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var name = "%sKafkaConsumer".formatted(owner.simpleName());
        var packageName = new TypeManifest(eventHandlers.getFirst().getEnclosingElement().asType(),
                context.processingEnvironment()).packageName();
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, Modifier.STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(packageName + ".infrastructure.kafka", name))
                        .build());

        var fields = support.addFields(eventHandlers, context, type);
        addEventHandlers(eventHandlers, owner, context, type);
        var topics = eventHandlers.stream()
                .map(e -> support.rootEventType(e, context).annotationsOfType(Event.class).stream().findFirst()
                        .orElseThrow()
                        .topic())
                .collect(Collectors.toSet());
        type.addMethod(constructor(topics, fields, eventHandlers, context));
        fileWriter.writeFile(packageName, name, type.build());
    }

    private void addEventHandlers(
            List<ExecutableElement> allEventHandlers,
            TypeManifest owner,
            PrefabContext context,
            TypeSpec.Builder type
    ) {
        var eventHandlersByEventType = allEventHandlers.stream()
                .collect(groupingBy(e -> support.rootEventType(e, context)));
        for (Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent : eventHandlersByEventType.entrySet()) {
            var eventType = eventHandlersForEvent.getKey();
            var annotation = eventType.annotationsOfType(Event.class).stream().findFirst().orElseThrow();
            var eventName = eventType.simpleName().replace(".", "");
            var method = MethodSpec.methodBuilder("on%s".formatted(eventName))
                    .addModifiers(PUBLIC)
                    .addAnnotation(kafkaListener(owner, annotation, eventName))
                    .addParameter(eventType.asTypeName(), "event")
                    .addStatement("log.debug($S, event)", "Received event {}");
            support.writeEventHandler(context, type, eventHandlersForEvent, eventType, method);
        }
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
        var eventType = support.eventTypeOf(eventHandlers, context, topic);
        var topicVariableName = uncapitalize(eventType.simpleName().replace(".", "")) + "Topic";
        if (topic.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, topicVariableName)
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("typeResolver.registerType($L, $T.class)", topicVariableName, eventType.asTypeName());
        } else {
            constructor.addStatement("typeResolver.registerType($S, $T.class)", topic, eventType.asTypeName());
        }
    }
}
