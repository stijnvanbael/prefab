package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

/**
 * Generates a {@code *KafkaEventTypeRegistrar} Spring component for each Kafka event type.
 *
 * <p>Registrars ensure that event types are registered with {@link KafkaJsonTypeResolver} once,
 * independently of whether a production consumer exists. This makes event types available for
 * deserialization in test consumers as well as production consumers.
 */
class KafkaEventTypeRegistrarWriter {

    private final PrefabContext context;

    KafkaEventTypeRegistrarWriter(PrefabContext context) {
        this.context = context;
    }

    /**
     * Writes a registrar component for a regular (non-Avro) Kafka event type.
     *
     * @param event the event type manifest
     */
    void writeRegistrar(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> KafkaPlugin.platformIsKafka(e, event.asElement(), context))
                .findFirst()
                .orElseThrow();
        var simpleName = event.simpleName().replace(".", "");
        var name = registrarName(simpleName);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addMethod(constructor(annotation.topic(), simpleName, event.asTypeName()))
                .build();
        fileWriter.writeFile(event.packageName(), name, type);
    }

    /**
     * Writes a registrar component for an Avro-generated Kafka event type.
     *
     * @param packageName the package name for the generated registrar
     * @param eventType   the Avro-generated event class name
     * @param topic       the Kafka topic
     */
    void writeAvscRegistrar(String packageName, ClassName eventType, String topic) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");
        var name = registrarName(eventType.simpleName());
        var type = TypeSpec.classBuilder(name)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addMethod(constructor(topic, eventType.simpleName(), eventType))
                .build();
        fileWriter.writeFile(packageName, name, type);
    }

    private MethodSpec constructor(String topic, String simpleName, TypeName eventTypeName) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(KafkaJsonTypeResolver.class, "typeResolver");
        if (topic.matches("\\$\\{.+}")) {
            var topicFieldName = topicVariableName(simpleName);
            constructor.addParameter(ParameterSpec.builder(String.class, topicFieldName)
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", topic)
                            .build())
                    .build());
            constructor.addStatement("typeResolver.registerType($L, $T.class)", topicFieldName, eventTypeName);
        } else {
            constructor.addStatement("typeResolver.registerType($S, $T.class)", topic, eventTypeName);
        }
        return constructor.build();
    }

    private static String registrarName(String simpleName) {
        return "%sKafkaEventTypeRegistrar".formatted(simpleName);
    }

    private static String topicVariableName(String simpleName) {
        return uncapitalize(simpleName) + "Topic";
    }
}
