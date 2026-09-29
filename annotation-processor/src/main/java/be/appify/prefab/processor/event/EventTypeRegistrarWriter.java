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
     * @param topics      the topic strings (may contain {@code ${...}} placeholders or {@code #{...}} SpEL expressions)
     * @param publishTo   the publish-to strategy
     */
    public void writeAvscRegistrar(String packageName, ClassName eventType, String[] topics, PublishTo publishTo) {
        writeAvscRegistrar(packageName, eventType, topics, publishTo, Optional.empty());
    }

    public void writeAvscRegistrar(String packageName, ClassName eventType, String[] topics,
                                   PublishTo publishTo, Optional<CodeBlock> keyExtractor) {
        var name = registrarName(eventType.simpleName());
        var type = buildRegistrarType(packageName, name, topics, Event.Serialization.AVRO,
                publishTo, eventType.simpleName(), eventType, keyExtractor);
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

        // Use indexed field names (e.g. myEventTopics0, myEventTopics1) when there are multiple topics;
        // preserve the non-indexed name (myEventTopics) for the common single-topic case.
        boolean useIndexedNames = topics.length > 1;

        if (Arrays.stream(topics).anyMatch(EventTypeRegistrarWriter::isExpression)) {
            var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            for (int i = 0; i < topics.length; i++) {
                var topic = topics[i];
                if (isExpression(topic)) {
                    var fieldName = topicFieldName(simpleName, i, useIndexedNames);
                    typeBuilder.addField(FieldSpec.builder(String[].class, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                    constructor.addParameter(ParameterSpec.builder(String[].class, fieldName)
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

    /**
     * A topic containing a property placeholder or SpEL expression is resolved by Spring at runtime.
     * It is injected as a {@code String[]} because an expression may expand to multiple topics
     * (an array, a collection or a comma-separated string).
     */
    private static boolean isExpression(String topic) {
        return topic.contains("${") || topic.contains("#{");
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
            if (isExpression(topic)) {
                method.beginControlFlow("for (var topic : $L)", topicFieldName(simpleName, i, useIndexedNames));
                method.addStatement(registerStatement(CodeBlock.of("topic"), serialization, eventTypeName, keyExtractor));
                method.endControlFlow();
            } else {
                method.addStatement(registerStatement(CodeBlock.of("$S", topic), serialization, eventTypeName, keyExtractor));
            }
        }

        if (publishTo != PublishTo.FIRST) {
            method.addStatement("registry.registerPublishTo($T.class, $T.$L)",
                    eventTypeName, PublishTo.class, publishTo.name());
        }

        return method.build();
    }

    private static CodeBlock registerStatement(CodeBlock topic, Event.Serialization serialization,
                                               TypeName eventTypeName, Optional<CodeBlock> keyExtractor) {
        return keyExtractor
                .map(extractor -> CodeBlock.of("registry.register($L, $T.class, $T.$L, event -> $L)",
                        topic, eventTypeName, Event.Serialization.class, serialization, extractor))
                .orElseGet(() -> CodeBlock.of("registry.register($L, $T.class, $T.$L)",
                        topic, eventTypeName, Event.Serialization.class, serialization));
    }

    /**
     * Derives the field name for an injected topic expression.
     *
     * <p>Single topic → {@code {simpleName}Topics}.
     * Multiple topics → {@code {simpleName}Topics{index}}.
     */
    private static String topicFieldName(String simpleName, int index, boolean useIndexedNames) {
        var base = uncapitalize(simpleName) + "Topics";
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
