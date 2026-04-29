package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** A plugin for generating Avro converters and schema factories for events annotated with {@link Event} with Avro serialization. */
public class AvroPlugin implements PrefabPlugin {
    private EventToGenericRecordConverterWriter toGenericRecordConverterWriter;
    private GenericRecordToEventConverterWriter toEventConverterWriter;
    private EventSchemaFactoryWriter eventSchemaFactoryWriter;
    private PrefabContext context;


    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        toGenericRecordConverterWriter = new EventToGenericRecordConverterWriter(context);
        toEventConverterWriter = new GenericRecordToEventConverterWriter(context);
        eventSchemaFactoryWriter = new EventSchemaFactoryWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        // Use context.eventElements() which already merges @Event-annotated types and AVSC-generated
        // records. For non-AVSC events we still restrict to those with AVRO serialization.
        var events = context.eventElements()
                .filter(e -> EventPlatformPluginSupport.isAvscGeneratedRecord(e)
                        || Objects.requireNonNull(e.getAnnotation(Event.class)).serialization() == Event.Serialization.AVRO)
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();

        events.forEach(event -> {
            toGenericRecordConverterWriter.writeConverter(event);
            toEventConverterWriter.writeConverter(event);
            eventSchemaFactoryWriter.writeSchemaFactory(event);
        });
        allNestedTypes(events)
                .forEach(type -> {
                    toGenericRecordConverterWriter.writeConverter(type);
                    toEventConverterWriter.writeConverter(type);
                    eventSchemaFactoryWriter.writeSchemaFactory(type);
                });
        sealedSubtypes(events).forEach(type -> {
            toGenericRecordConverterWriter.writeConverter(type);
            toEventConverterWriter.writeConverter(type);
            eventSchemaFactoryWriter.writeSchemaFactory(type);
        });
    }

    static List<TypeManifest> nestedTypes(List<TypeManifest> events) {
        return Stream.concat(events.stream(), sealedSubtypes(events).stream())
                .flatMap(event -> event.fields().stream())
                .map(VariableManifest::type)
                .filter(type -> isNestedRecord(type) || isListOfNestedRecord(type))
                .map(type -> isListOfNestedRecord(type) ? type.parameters().getFirst() : type)
                .distinct()
                .toList();
    }

    static List<TypeManifest> allNestedTypes(List<TypeManifest> events) {
        var toProcess = new ArrayDeque<>(nestedTypes(events));
        var result = new ArrayList<>(toProcess);
        while (!toProcess.isEmpty()) {
            var type = toProcess.poll();
            nestedTypes(List.of(type)).stream()
                    .filter(t -> !result.contains(t))
                    .forEach(t -> {
                        result.add(t);
                        toProcess.add(t);
                    });
        }
        return result;
    }

    static List<TypeManifest> sealedSubtypes(List<TypeManifest> events) {
        return events.stream()
                .flatMap(event -> event.permittedSubtypes().stream())
                .distinct()
                .toList();
    }

    private static boolean isListOfNestedRecord(TypeManifest type) {
        return type.is(List.class) && isNestedRecord(type.parameters().getFirst());
    }

    static boolean isNestedRecord(TypeManifest type) {
        return !type.isStandardType() && !type.isEnum() && !isLogicalType(type) && !type.isCustomType();
    }

    static boolean isLogicalType(TypeManifest type) {
        return type.is(Instant.class)
                || type.is(LocalDate.class)
                || type.is(Duration.class)
                || (type.isSingleValueType() && type.fields().getFirst().type().isStandardType());
    }
}
