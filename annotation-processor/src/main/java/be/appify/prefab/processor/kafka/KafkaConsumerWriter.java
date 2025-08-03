package be.appify.prefab.processor.kafka;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

public class KafkaConsumerWriter {

    public void writeKafkaConsumer(ExecutableElement eventHandler, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var target = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
        var event = eventType(eventHandler, context);
        var name = "%s%sKafkaConsumer".formatted(target.simpleName(), event.simpleName());
        var annotation = event.annotationsOfType(Event.class).stream().findFirst().orElseThrow();
        TypeSpec type;

        if (!target.annotationsOfType(Aggregate.class).isEmpty()) {
            type = writeAggregateConsumer(target, name, event, annotation);
        } else if (!target.annotationsOfType(Component.class).isEmpty()) {
            type = writeComponentConsumer(target, name, event, annotation, eventHandler);
        } else {
            throw new IllegalStateException("Cannot write Kafka consumer for %s, it is neither an Aggregate nor a Component".formatted(target.simpleName()));
        }

        fileWriter.writeFile(target.packageName(), name, type);
    }

    private TypeSpec writeComponentConsumer(TypeManifest component, String name, TypeManifest event, Event annotation, ExecutableElement eventHandler) {
        return TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, Modifier.STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(component.packageName() + ".infrastructure.kafka", name))
                        .build())
                .addField(component.asTypeName(), "component", PRIVATE, FINAL)
                .addMethod(componentConstructor(component, event, annotation))
                .addMethod(componentEventListener(event, eventHandler))
                .build();
    }

    private static MethodSpec componentEventListener(TypeManifest event, ExecutableElement eventHandler) {
        return MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                .addModifiers(PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addStatement("log.debug($S, event)", "Received event {}")
                .addStatement("component.$L(event)", eventHandler.getSimpleName())
                .build();
    }

    private static TypeSpec writeAggregateConsumer(TypeManifest aggregate, String name, TypeManifest event, Event annotation) {
        var serviceClass = ClassName.get(
                "%s.application".formatted(aggregate.packageName()),
                "%sService".formatted(aggregate.simpleName())
        );
        return TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, Modifier.STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(aggregate.packageName() + ".infrastructure.kafka", name))
                        .build())
                .addField(serviceClass, "service", PRIVATE, FINAL)
                .addMethod(aggregateConstructor(serviceClass, event, annotation))
                .addMethod(aggregateEventListener(event, annotation))
                .build();
    }

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().stream()
                .filter(parameter -> new TypeManifest(parameter.asType(), context.processingEnvironment()).annotationsOfType(Event.class)
                        .stream().anyMatch(event -> event.platform() == Event.Platform.KAFKA))
                .findFirst()
                .orElseThrow()
                .asType(), context.processingEnvironment());
    }

    private static MethodSpec aggregateConstructor(ClassName serviceClass, TypeManifest event, Event annotation) {
        return MethodSpec.constructorBuilder()
                .addParameter(serviceClass, "service")
                .addParameter(KafkaJsonTypeResolver.class, "typeResolver")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", annotation.topic())
                                .build())
                        .build())
                .addStatement("this.service = service")
                .addStatement("typeResolver.registerType(topic, $T.class)", event.asTypeName())
                .build();
    }

    private static MethodSpec componentConstructor(TypeManifest component, TypeManifest event, Event annotation) {
        return MethodSpec.constructorBuilder()
                .addParameter(component.asTypeName(), "component")
                .addParameter(KafkaJsonTypeResolver.class, "typeResolver")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", annotation.topic())
                                .build())
                        .build())
                .addStatement("this.component = component")
                .addStatement("typeResolver.registerType(topic, $T.class)", event.asTypeName())
                .build();
    }

    private static MethodSpec aggregateEventListener(TypeManifest event, Event annotation) {
        return MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(KafkaListener.class)
                        .addMember("topics", "$S", annotation.topic())
                        .build())
                .addParameter(event.asTypeName(), "event")
                .addStatement("service.on$L(event)", event.simpleName())
                .build();
    }
}
