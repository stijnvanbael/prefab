package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.annotations.Event;
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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

public class PubSubConsumerWriter {
    public void writePubSubConsumer(ExecutableElement eventHandler, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.pubsub");

        var aggregate = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
        var event = eventType(eventHandler, context);
        var name = "%s%sPubSubConsumer".formatted(aggregate.simpleName(), event.simpleName());
        var annotation = event.annotationsOfType(Event.class).stream().findFirst().orElseThrow();
        var serviceClass = ClassName.get(
                "%s.application".formatted(aggregate.packageName()),
                "%sService".formatted(aggregate.simpleName())
        );
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(event.packageName() + ".infrastructure.pubsub", name))
                        .build())
                .addField(serviceClass, "service", PRIVATE, FINAL)
                .addMethod(constructor(aggregate, serviceClass, event, annotation))
                .addMethod(eventListener(event))
                .build();

        fileWriter.writeFile(aggregate.packageName(), name, type);
    }

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().stream()
                .filter(parameter -> new TypeManifest(parameter.asType(), context.processingEnvironment()).annotationsOfType(Event.class)
                        .stream().anyMatch(event -> event.platform() == Event.Platform.PUB_SUB))
                .findFirst()
                .orElseThrow()
                .asType(), context.processingEnvironment());
    }

    private static MethodSpec constructor(TypeManifest aggregate, ClassName serviceClass, TypeManifest event, Event annotation) {
        return MethodSpec.constructorBuilder()
                .addParameter(serviceClass, "service")
                .addParameter(PubSubUtil.class, "pubSub")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", annotation.topic())
                                .build())
                        .build())
                .addStatement("this.service = service")
                .addStatement("pubSub.subscribe(topic, $S, $T.class, this::on$L)",
                        CaseUtil.toKebabCase(aggregate.simpleName()) + "-on-" + CaseUtil.toKebabCase(event.simpleName()),
                        event.asTypeName(),
                        event.simpleName())
                .build();
    }

    private static MethodSpec eventListener(TypeManifest event) {
        return MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                .addModifiers(PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addStatement("log.debug($S, event)", "Received event {}")
                .addStatement("service.on$L(event)", event.simpleName())
                .build();
    }
}
