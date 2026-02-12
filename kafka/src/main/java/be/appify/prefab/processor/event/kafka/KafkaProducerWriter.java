package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.Event;
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
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.ConsumerWriterSupport.keyField;
import static be.appify.prefab.processor.event.kafka.KafkaPlugin.platformIsKafka;
import static javax.lang.model.element.Modifier.PUBLIC;

class KafkaProducerWriter {
    private final PrefabContext context;

    KafkaProducerWriter(PrefabContext context) {
        this.context = context;
    }

    void writeKafkaProducer(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var name = "%sKafkaProducer".formatted(event.simpleName().replace(".", ""));
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> platformIsKafka(e, event.asElement(), context))
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
        if (topic.matches("\\$\\{.+}")) {
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
        var method = MethodSpec.methodBuilder("publish")
                .addModifiers(PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addAnnotation(EventListener.class)
                .addStatement("log.debug($S, event, topic)", "Publishing event {} on topic {}");
        keyField(event, context).ifPresentOrElse(
                keyField -> method.addStatement("kafkaTemplate.send(topic, $L, event)", keyField),
                () -> method.addStatement("kafkaTemplate.send(topic, event)"));
        return method.build();
    }
}
