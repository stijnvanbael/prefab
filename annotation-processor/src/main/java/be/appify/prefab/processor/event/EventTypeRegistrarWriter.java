package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.processor.OutputTargetFileOutput;
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
import java.util.Arrays;
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
    private final OutputTargetFileOutput fileOutput;

    public EventTypeRegistrarWriter(PrefabContext context) {
        this.context = context;
        this.fileOutput = new OutputTargetFileOutput(context, "infrastructure.event", OutputTarget.MAIN);
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
        var type = buildRegistrarType(event.packageName(), name, annotation.topic(), annotation.serialization(),
                annotation.publishTo(), simpleName, event.asTypeName(), keyField(event, context));
        fileOutput.writeFile(event.packageName(), name, type);
    }

    /**
     * Writes a registrar component for a schema-generated (e.g. Avro) event type.
     *
     * @param packageName the base package for the generated registrar
     * @param eventType   the generated event class name
     * @param topics      the topic strings (may contain {@code ${...}} placeholders)
     * @param publishTo   the publish-to strategy
     */
    public void writeAvscRegistrar(String packageName, ClassName eventType, String[] topics, PublishTo publishTo) {
        var name = registrarName(eventType.simpleName());
        var type = buildRegistrarType(packageName, name, topics, Event.Serialization.AVRO,
                publishTo, eventType.simpleName(), eventType, Optional.empty());
        fileOutput.writeFile(packageName, name, type);
    }

    private TypeSpec buildRegistrarType(String packageName, String name, String[] topics,
                                        Event.Serialization serialization,
                                        PublishTo publishTo, String simpleName, TypeName eventTypeName,
                                        Optional<CodeBlock> keyExtractor) {
        var typeBuilder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(packageName, name))
                .addSuperinterface(EventRegistryCustomizer.class);

        var placeholderTopics = Arrays.stream(topics)
                .filter(t -> t.matches("\\$\\{.+}"))
                .toList();

        // Use indexed field names (e.g. myEventTopic0, myEventTopic1) when there are multiple topics;
        // preserve the non-indexed name (myEventTopic) for the common single-topic case.
        boolean useIndexedNames = topics.length > 1;

        if (!placeholderTopics.isEmpty()) {
            var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            for (int i = 0; i < topics.length; i++) {
                var topic = topics[i];
                if (topic.matches("\\$\\{.+}")) {
                    var fieldName = topicFieldName(simpleName, i, useIndexedNames);
                    typeBuilder.addField(FieldSpec.builder(String.class, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                    constructor.addParameter(ParameterSpec.builder(String.class, fieldName)
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build());
                    constructor.addStatement("this.$L = $L", fieldName, fieldName);
                }
            }
            typeBuilder.addMethod(constructor.build());
        }

        typeBuilder.addMethod(customizeMethod(topics, serialization, simpleName, eventTypeName, keyExtractor, useIndexedNames, publishTo));
        return typeBuilder.build();
    }

    private static MethodSpec customizeMethod(String[] topics, Event.Serialization serialization,
                                               String simpleName, TypeName eventTypeName,
                                               Optional<CodeBlock> keyExtractor,
                                               boolean useIndexedNames, PublishTo publishTo) {
        var method = MethodSpec.methodBuilder("customize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(EventRegistry.class, "registry");

        for (int i = 0; i < topics.length; i++) {
            var topic = topics[i];
            boolean isPlaceholder = topic.matches("\\$\\{.+}");
            String topicArg = isPlaceholder
                    ? topicFieldName(simpleName, i, useIndexedNames)
                    : "\"" + topic + "\"";

            if (keyExtractor.isPresent()) {
                method.addStatement("registry.register($L, $T.class, $T.$L, event -> $L)",
                        topicArg, eventTypeName, Event.Serialization.class, serialization,
                        keyExtractor.get());
            } else {
                method.addStatement("registry.register($L, $T.class, $T.$L)",
                        topicArg, eventTypeName, Event.Serialization.class, serialization);
            }
        }

        if (publishTo != PublishTo.FIRST) {
            method.addStatement("registry.registerPublishTo($T.class, $T.$L)",
                    eventTypeName, PublishTo.class, publishTo.name());
        }

        return method.build();
    }

    /**
     * Derives the field name for a topic parameter.
     *
     * <p>Single topic → {@code {simpleName}Topic} (backward-compatible).
     * Multiple topics → {@code {simpleName}Topic{index}}.
     */
    private static String topicFieldName(String simpleName, int index, boolean useIndexedNames) {
        var base = uncapitalize(simpleName) + "Topic";
        return useIndexedNames ? base + index : base;
    }

    private static String registrarName(String simpleName) {
        return "%sEventTypeRegistrar".formatted(simpleName);
    }

    /**
     * Builds a {@code @Component} annotation whose value is a fully qualified bean name derived from
     * the event's package and the generated class name.  This prevents Spring context conflicts when
     * multiple events share the same simple name but live in different packages.
     *
     * <p>Example: package {@code com.example.order}, class {@code OrderCreatedEventTypeRegistrar}
     * → bean name {@code com_example_order_OrderCreatedEventTypeRegistrar}.
     */
    private static AnnotationSpec componentAnnotation(String packageName, String name) {
        return AnnotationSpec.builder(Component.class)
                .addMember("value", "$S", packageName.replace('.', '_') + "_" + name)
                .build();
    }
}

