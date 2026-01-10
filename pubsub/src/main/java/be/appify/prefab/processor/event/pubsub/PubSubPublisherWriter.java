package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
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

import static be.appify.prefab.processor.event.pubsub.PubSubPlugin.platformIsPubSub;

class PubSubPublisherWriter {
    void writePubSubPublisher(TypeManifest event, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.pubsub");

        var name = "%sPubSubPublisher".formatted(event.simpleName().replace(".", ""));
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> platformIsPubSub(e, event.asElement(), context))
                .findFirst().orElseThrow();
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(event.packageName() + ".infrastructure.pubsub", name))
                        .build())
                .addField(PubSubTemplate.class, "pubSubTemplate", Modifier.PRIVATE, Modifier.FINAL)
                .addField(JsonUtil.class, "jsonSupport", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "topic", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor(annotation.topic()))
                .addMethod(producer(event))
                .build();

        fileWriter.writeFile(event.packageName(), name, type);
    }

    private MethodSpec constructor(String topic) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(PubSubTemplate.class, "pubSubTemplate")
                .addParameter(PubSubUtil.class, "pubSub")
                .addParameter(JsonUtil.class, "jsonSupport")
                .addStatement("this.pubSubTemplate = pubSubTemplate")
                .addStatement("this.jsonSupport = jsonSupport");
        if (topic.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, "topic")
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("this.topic = pubSub.ensureTopicExists(topic)");
        } else {
            constructor.addStatement("this.topic = pubSub.ensureTopicExists($S)", topic);
        }
        return constructor.build();
    }

    private MethodSpec producer(TypeManifest event) {
        var keyField = event.methodsWith(PartitioningKey.class).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Event %s does not have a field annotated with @Key".formatted(event.simpleName())))
                .getSimpleName().toString(); // TODO: add ordering key
        return MethodSpec.methodBuilder("publish")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addAnnotation(EventListener.class)
                .addStatement("log.debug($S, event, topic)", "Publishing event {} on topic {}")
                .addStatement("""
                                pubSubTemplate.publish(
                                    topic,
                                    $T.newBuilder()
                                        .setData($T.copyFromUtf8(jsonSupport.toJson(event)))
                                        .setOrderingKey(event.$L())
                                        .putAttributes($S, event.getClass().getName())
                                        .build())""",
                        PubsubMessage.class,
                        ByteString.class,
                        keyField,
                        "type"
                )
                .build();
    }
}
