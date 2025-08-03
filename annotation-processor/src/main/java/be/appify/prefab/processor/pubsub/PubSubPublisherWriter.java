package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Key;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.spring.JsonUtil;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;

public class PubSubPublisherWriter {
    public void writePubSubPublisher(ClassManifest event, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.pubsub");

        var name = "%sPubSubPublisher".formatted(event.simpleName());
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> e.platform() == Event.Platform.PUB_SUB)
                .findFirst().orElseThrow();
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(event.packageName() + ".infrastructure.pubsub", name))
                        .build())
                .addField(PubSubTemplate.class, "pubSubTemplate", Modifier.PRIVATE, Modifier.FINAL)
                .addField(JsonUtil.class, "jsonSupport", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "topic", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor(annotation.topic()))
                .addMethod(publisher(event))
                .build();

        fileWriter.writeFile(event.packageName(), name, type);
    }

    private MethodSpec constructor(String topic) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(PubSubTemplate.class, "pubSubTemplate")
                .addParameter(PubSubUtil.class, "pubSub")
                .addParameter(JsonUtil.class, "jsonSupport")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", topic)
                                .build())
                        .build())
                .addStatement("this.pubSubTemplate = pubSubTemplate")
                .addStatement("this.jsonSupport = jsonSupport")
                .addStatement("this.topic = pubSub.ensureTopicExists(topic)")
                .build();
    }

    private MethodSpec publisher(ClassManifest event) {
        var keyField = event.fields().stream()
                .filter(field -> field.hasAnnotation(Key.class))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Event %s does not have a field annotated with @Key".formatted(event.simpleName())))
                .name();
        return MethodSpec.methodBuilder("publish")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(event.type().asTypeName(), "event")
                .addAnnotation(EventListener.class)
                .addStatement("log.debug($S, event, topic)", "Publishing event {} on topic {}")
                .addStatement("pubSubTemplate.publish(topic, jsonSupport.toJson(event))", keyField)
                .build();
    }
}
