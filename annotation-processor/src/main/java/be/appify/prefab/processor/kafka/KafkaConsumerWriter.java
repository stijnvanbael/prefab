package be.appify.prefab.processor.kafka;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

public class KafkaConsumerWriter {

    public void writeKafkaConsumer(
            String topic,
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
        var fields = addFields(eventHandlers, context, type);
        addEventHandlers(eventHandlers, context, type);
        type.addMethod(constructor(topic, fields, eventHandlers, context));
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
                        "Cannot write Kafka consumer for %s, it is neither an Aggregate nor a Component".formatted(
                                target.simpleName()),
                        eventHandler);
            }
        }
        return fields;
    }

    private void addEventHandlers(List<ExecutableElement> eventHandlers, PrefabContext context, TypeSpec.Builder type) {
        for (ExecutableElement eventHandler : eventHandlers) {
            var event = eventType(eventHandler, context);
            var target = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
            var annotation = event.annotationsOfType(Event.class).stream().findFirst().orElseThrow();

            if (!target.annotationsOfType(Aggregate.class).isEmpty()) {
                type.addMethod(aggregateConsumer(target, event, annotation));
            } else if (!target.annotationsOfType(Component.class).isEmpty()) {
                type.addMethod(componentConsumer(target, event, annotation, eventHandler));
            } else {
                context.logError(
                        "Cannot write Kafka consumer for %s, it is neither an Aggregate nor a Component".formatted(
                                target.simpleName()),
                        eventHandler);
            }
        }
    }

    private MethodSpec componentConsumer(TypeManifest target, TypeManifest event, Event annotation,
            ExecutableElement eventHandler) {
        return MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                .addModifiers(PUBLIC)
                .addAnnotation(AnnotationSpec.builder(KafkaListener.class)
                        .addMember("topics", "$S", annotation.topic())
                        .build())
                .addParameter(event.asTypeName(), "event")
                .addStatement("log.debug($S, event)", "Received event {}")
                .addStatement("$N.$L(event)", uncapitalize(target.simpleName()), eventHandler.getSimpleName())
                .build();
    }

    private static MethodSpec aggregateConsumer(TypeManifest target, TypeManifest event, Event annotation) {
        return MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                .addModifiers(PUBLIC)
                .addAnnotation(AnnotationSpec.builder(KafkaListener.class)
                        .addMember("topics", "$S", annotation.topic())
                        .build())
                .addParameter(event.asTypeName(), "event")
                .addStatement("log.debug($S, event)", "Received event {}")
                .addStatement("$NService.on$L(event)", uncapitalize(target.simpleName()), event.simpleName())
                .build();
    }

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().stream()
                .filter(parameter -> new TypeManifest(parameter.asType(),
                        context.processingEnvironment()).annotationsOfType(Event.class)
                        .stream().anyMatch(event -> event.platform() == Event.Platform.KAFKA))
                .findFirst()
                .orElseThrow()
                .asType(), context.processingEnvironment());
    }

    private static MethodSpec constructor(
            String topic,
            Set<FieldSpec> fields,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var constructor = MethodSpec.constructorBuilder();
        fields.forEach(field -> constructor.addParameter(ParameterSpec.builder(field.type(), field.name()).build()));
        constructor
                .addParameter(KafkaJsonTypeResolver.class, "typeResolver")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", topic)
                                .build())
                        .build());
        fields.forEach(field -> constructor.addStatement("this.$L = $L", field.name(), field.name()));
        eventHandlers.forEach(eventHandler -> constructor.addStatement("typeResolver.registerType(topic, $T.class)",
                eventType(eventHandler, context).asTypeName()));
        return constructor.build();
    }
}
