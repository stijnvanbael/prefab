package be.appify.prefab.processor.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Key;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;

public class KafkaPublisherWriter {
    public void writeKafkaPublisher(ClassManifest event, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var name = "%sKafkaPublisher".formatted(event.simpleName());
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> e.platform() == Event.Platform.KAFKA)
                .findFirst().orElseThrow();
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(ParameterizedTypeName.get(
                                ClassName.get(KafkaTemplate.class),
                                ClassName.get(String.class),
                                ClassName.get(Object.class)
                        ),
                        "kafkaTemplate", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "topic", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor(annotation.topic()))
                .addMethod(publisher(event))
                .build();

        fileWriter.writeFile(event.packageName(), name, type);
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
                .addStatement("kafkaTemplate.send(topic, event.$L(), event)", keyField)
                .build();
    }

    private static MethodSpec constructor(String topic) {
        return MethodSpec.constructorBuilder()
                .addParameter(
                        ParameterizedTypeName.get(
                                ClassName.get(KafkaTemplate.class),
                                ClassName.get(String.class),
                                ClassName.get(Object.class)
                        ),
                        "kafkaTemplate")
                .addParameter(ParameterSpec.builder(String.class, "topic")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", topic)
                                .build())
                        .build())
                .addStatement("this.kafkaTemplate = kafkaTemplate")
                .addStatement("this.topic = topic")
                .build();
    }
}
