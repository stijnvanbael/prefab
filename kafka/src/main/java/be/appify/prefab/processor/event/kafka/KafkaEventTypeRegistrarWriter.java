package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.ConsumerWriterSupport.keyField;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

/**
 * Generates a {@code *KafkaEventTypeRegistrar} Spring component for each Kafka event type.
 *
 * <p>The generated class implements {@link EventRegistryCustomizer} and is picked up by
 * {@code PrefabRegistryConfiguration}, which applies all customizers atomically before the
 * {@link EventRegistry} is exposed to any consumer bean.
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
        var type = buildRegistrarType(name, annotation.topic(), annotation.serialization(),
                simpleName, event.asTypeName(), keyField(event, context));
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
        var type = buildRegistrarType(name, topic, Event.Serialization.AVRO,
                eventType.simpleName(), eventType, Optional.empty());
        fileWriter.writeFile(packageName, name, type);
    }

    private TypeSpec buildRegistrarType(String name, String topic, Event.Serialization serialization,
                                        String simpleName, TypeName eventTypeName,
                                        Optional<CodeBlock> keyExtractor) {
        var typeBuilder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addSuperinterface(EventRegistryCustomizer.class);

        boolean hasPlaceholderTopic = topic.matches("\\$\\{.+}");

        if (hasPlaceholderTopic) {
            var topicFieldName = topicVariableName(simpleName);
            typeBuilder.addField(FieldSpec.builder(String.class, topicFieldName, Modifier.PRIVATE, Modifier.FINAL).build());
            var constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(String.class, topicFieldName)
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("this.$L = $L", topicFieldName, topicFieldName);
            typeBuilder.addMethod(constructor.build());
        }

        typeBuilder.addMethod(buildCustomizeMethod(topic, serialization, simpleName, eventTypeName, keyExtractor, hasPlaceholderTopic));

        return typeBuilder.build();
    }

    private static MethodSpec buildCustomizeMethod(String topic, Event.Serialization serialization,
                                                    String simpleName, TypeName eventTypeName,
                                                    Optional<CodeBlock> keyExtractor,
                                                    boolean hasPlaceholderTopic) {
        var method = MethodSpec.methodBuilder("customize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(EventRegistry.class, "registry");

        var topicArg = hasPlaceholderTopic ? topicVariableName(simpleName) : "\"" + topic + "\"";

        if (keyExtractor.isPresent()) {
            method.addStatement("registry.register($L, $T.class, $T.$L, event -> $L)",
                    topicArg, eventTypeName, Event.Serialization.class, serialization.toString(),
                    keyExtractor.get());
        } else {
            method.addStatement("registry.register($L, $T.class, $T.$L)",
                    topicArg, eventTypeName, Event.Serialization.class, serialization.toString());
        }

        return method.build();
    }

    private static String registrarName(String simpleName) {
        return "%sKafkaEventTypeRegistrar".formatted(simpleName);
    }

    private static String topicVariableName(String simpleName) {
        return uncapitalize(simpleName) + "Topic";
    }
}
