package be.appify.prefab.processor.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;

import static javax.lang.model.element.Modifier.PUBLIC;

class KafkaProducerWriter {
    void writeKafkaProducer(TypeManifest event, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var name = "%sKafkaProducer".formatted(event.simpleName().replace(".", ""));
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> e.platform() == Event.Platform.KAFKA)
                .findFirst().orElseThrow();
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(event.packageName() + ".infrastructure.kafka", name))
                        .build())
                .addField(ParameterizedTypeName.get(
                                ClassName.get(KafkaTemplate.class),
                                ClassName.get(String.class),
                                ClassName.get(Object.class)
                        ),
                        "kafkaTemplate", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "topic", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor(annotation.topic()))
                .addMethod(producer(event))
                .build();

        fileWriter.writeFile(event.packageName(), name, type);
    }

    private static MethodSpec constructor(String topic) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                ClassName.get(KafkaTemplate.class),
                                ClassName.get(String.class),
                                ClassName.get(Object.class)
                        ),
                        "kafkaTemplate")
                .addStatement("this.kafkaTemplate = kafkaTemplate");
        if(topic.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, "topic")
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("this.topic = topic");
        } else {
            constructor.addStatement("this.topic = $S", topic);
        }
        return constructor.build();
    }

    private MethodSpec producer(TypeManifest event) {
        var keyField = event.methodsWith(PartitioningKey.class).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Event %s does not have a field annotated with @PartitioningKey".formatted(event.simpleName())))
                .getSimpleName().toString();
        return MethodSpec.methodBuilder("publish")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addAnnotation(EventListener.class)
                .addStatement("log.debug($S, event, topic)", "Publishing event {} on topic {}")
                .addStatement("kafkaTemplate.send(topic, event.$L(), event)", keyField)
                .build();
    }
}
