package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.annotations.Aggregate;
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
import com.palantir.javapoet.TypeName;
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

        var target = new TypeManifest(eventHandler.getEnclosingElement().asType(), context.processingEnvironment());
        var event = eventType(eventHandler, context);
        var name = "%s%sPubSubConsumer".formatted(target.simpleName(), event.simpleName());
        var annotation = event.annotationsOfType(Event.class).stream().findFirst().orElseThrow();
        TypeSpec type;

        if (!target.annotationsOfType(Aggregate.class).isEmpty()) {
            type = writeAggregateConsumer(target, name, event, annotation);
        } else if (!target.annotationsOfType(Component.class).isEmpty()) {
            type = writeComponentConsumer(target, name, event, annotation, eventHandler);
        } else {
            throw new IllegalStateException("Cannot write PubSub consumer for %s, it is neither an Aggregate nor a Component".formatted(target.simpleName()));
        }

        fileWriter.writeFile(target.packageName(), name, type);
    }

    private TypeSpec writeComponentConsumer(TypeManifest component, String name, TypeManifest event, Event annotation, ExecutableElement eventHandler) {
        return TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(component.packageName() + ".infrastructure.pubsub", name))
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
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(aggregate.packageName() + ".infrastructure.pubsub", name))
                        .build())
                .addField(serviceClass, "service", PRIVATE, FINAL)
                .addMethod(aggregateConstructor(aggregate, serviceClass, event, annotation))
                .addMethod(aggregateEventListener(event))
                .build();
    }

    private static TypeManifest eventType(ExecutableElement eventHandler, PrefabContext context) {
        return new TypeManifest(eventHandler.getParameters().stream()
                .filter(parameter -> new TypeManifest(parameter.asType(), context.processingEnvironment()).annotationsOfType(Event.class)
                        .stream().anyMatch(event -> event.platform() == Event.Platform.PUB_SUB))
                .findFirst()
                .orElseThrow()
                .asType(), context.processingEnvironment());
    }

    private static MethodSpec aggregateConstructor(TypeManifest aggregate, TypeName serviceClass, TypeManifest event, Event annotation) {
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

    private static MethodSpec componentConstructor(TypeManifest component, TypeManifest event, Event annotation) {
        return MethodSpec.constructorBuilder()
                .addParameter(component.asTypeName(), "component")
                .addParameter(PubSubUtil.class, "pubSub")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", annotation.topic())
                                .build())
                        .build())
                .addStatement("this.component = component")
                .addStatement("pubSub.subscribe(topic, $S, $T.class, this::on$L)",
                        CaseUtil.toKebabCase(component.simpleName()) + "-on-" + CaseUtil.toKebabCase(event.simpleName()),
                        event.asTypeName(),
                        event.simpleName())
                .build();
    }

    private static MethodSpec aggregateEventListener(TypeManifest event) {
        return MethodSpec.methodBuilder("on%s".formatted(event.simpleName()))
                .addModifiers(PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addStatement("log.debug($S, event)", "Received event {}")
                .addStatement("service.on$L(event)", event.simpleName())
                .build();
    }
}
