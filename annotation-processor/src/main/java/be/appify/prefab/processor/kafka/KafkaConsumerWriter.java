package be.appify.prefab.processor.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

public class KafkaConsumerWriter {

    public void writeKafkaConsumer(ExecutableElement eventHandler, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var aggregate = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
        var event = eventType(eventHandler, context);
        var name = "%s%sKafkaConsumer".formatted(aggregate.simpleName(), event.simpleName());
        var annotation = event.annotationsOfType(Event.class).stream().findFirst().orElseThrow();
        var serviceClass = ClassName.get(
                "%s.application".formatted(aggregate.packageName()),
                "%sService".formatted(aggregate.simpleName())
        );
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(Modifier.PUBLIC)
                .addField(serviceClass, "service", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor(serviceClass, event, annotation))
                .addMethod(eventListener(event, annotation))
                .build();

        fileWriter.writeFile(aggregate.packageName(), name, type);
    }

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().stream()
                .filter(parameter -> new TypeManifest(parameter.asType(), context.processingEnvironment()).annotationsOfType(Event.class)
                        .stream().anyMatch(event -> event.platform() == Event.Platform.KAFKA))
                .findFirst()
                .orElseThrow()
                .asType(), context.processingEnvironment());
    }

    private static MethodSpec constructor(ClassName serviceClass, TypeManifest event, Event annotation) {
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

    private static MethodSpec eventListener(TypeManifest event, Event annotation) {
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
