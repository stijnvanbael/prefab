package be.appify.prefab.processor.event;

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
 * Generates an {@code *EventTypeRegistrar} Spring component for each {@link Event}-annotated type.
 *
 * <p>The generated class implements {@link EventRegistryCustomizer} and registers the event type,
 * topic, serialisation format, and — when a {@code @PartitioningKey} is present — a key extractor.
 * This writer is platform-neutral; broker-specific plugins (e.g. Kafka Avro) may call
 * {@link #writeAvscRegistrar} for schema-generated types.
 */
public class EventTypeRegistrarWriter {

    private final PrefabContext context;

    public EventTypeRegistrarWriter(PrefabContext context) {
        this.context = context;
    }

    /**
     * Writes a registrar component for a standard (source-level) event type.
     *
     * @param event the event type manifest
     */
    public void writeRegistrar(TypeManifest event) {
        var annotation = event.annotationsOfType(Event.class).stream()
                .findFirst()
                .orElseThrow();
        var simpleName = event.simpleName().replace(".", "");
        var name = registrarName(simpleName);
        var type = buildRegistrarType(name, annotation.topic(), annotation.serialization(),
                simpleName, event.asTypeName(), keyField(event, context));
        new JavaFileWriter(context.processingEnvironment(), "infrastructure.event")
                .writeFile(event.packageName(), name, type);
    }

    /**
     * Writes a registrar component for a schema-generated (e.g. Avro) event type.
     *
     * @param packageName the base package for the generated registrar
     * @param eventType   the generated event class name
     * @param topic       the topic string (may be a {@code ${...}} placeholder)
     */
    public void writeAvscRegistrar(String packageName, ClassName eventType, String topic) {
        var name = registrarName(eventType.simpleName());
        var type = buildRegistrarType(name, topic, Event.Serialization.AVRO,
                eventType.simpleName(), eventType, Optional.empty());
        new JavaFileWriter(context.processingEnvironment(), "infrastructure.event")
                .writeFile(packageName, name, type);
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
            var topicField = topicFieldName(simpleName);
            typeBuilder.addField(FieldSpec.builder(String.class, topicField, Modifier.PRIVATE, Modifier.FINAL).build());
            typeBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(String.class, topicField)
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("this.$L = $L", topicField, topicField)
                    .build());
        }

        typeBuilder.addMethod(customizeMethod(topic, serialization, simpleName, eventTypeName, keyExtractor, hasPlaceholderTopic));
        return typeBuilder.build();
    }

    private static MethodSpec customizeMethod(String topic, Event.Serialization serialization,
                                               String simpleName, TypeName eventTypeName,
                                               Optional<CodeBlock> keyExtractor,
                                               boolean hasPlaceholderTopic) {
        var method = MethodSpec.methodBuilder("customize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(EventRegistry.class, "registry");

        var topicArg = hasPlaceholderTopic ? topicFieldName(simpleName) : "\"" + topic + "\"";

        if (keyExtractor.isPresent()) {
            method.addStatement("registry.register($L, $T.class, $T.$L, event -> $L)",
                    topicArg, eventTypeName, Event.Serialization.class, serialization,
                    keyExtractor.get());
        } else {
            method.addStatement("registry.register($L, $T.class, $T.$L)",
                    topicArg, eventTypeName, Event.Serialization.class, serialization);
        }

        return method.build();
    }

    private static String registrarName(String simpleName) {
        return "%sEventTypeRegistrar".formatted(simpleName);
    }

    private static String topicFieldName(String simpleName) {
        return uncapitalize(simpleName) + "Topic";
    }
}

