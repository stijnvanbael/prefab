package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.google.common.collect.Streams;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** A plugin for generating Avro converters and schema factories for events annotated with {@link Event} with Avro serialization. */
public class AvroPlugin implements PrefabPlugin {
    private final EventToGenericRecordConverterWriter toGenericRecordConverterWriter = new EventToGenericRecordConverterWriter();
    private final GenericRecordToEventConverterWriter toEventConverterWriter = new GenericRecordToEventConverterWriter();
    private final EventSchemaFactoryWriter eventSchemaFactoryWriter = new EventSchemaFactoryWriter();

    /** Constructs a new AvroPlugin. */
    public AvroPlugin() {
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(event -> Objects.requireNonNull(event.getAnnotation(Event.class)).serialization() == Event.Serialization.AVRO)
                .map(element -> new TypeManifest(element.asType(), context.processingEnvironment()))
                .toList();
        events.forEach(event -> {
            toGenericRecordConverterWriter.writeConverter(event, context);
            toEventConverterWriter.writeConverter(event, context);
            eventSchemaFactoryWriter.writeSchemaFactory(event, context);
        });
        nestedTypes(events)
                .forEach(type -> {
                    toGenericRecordConverterWriter.writeConverter(type, context);
                    toEventConverterWriter.writeConverter(type, context);
                    eventSchemaFactoryWriter.writeSchemaFactory(type, context);
                });
        sealedSubtypes(events).forEach(type -> {
            toGenericRecordConverterWriter.writeConverter(type, context);
            toEventConverterWriter.writeConverter(type, context);
            eventSchemaFactoryWriter.writeSchemaFactory(type, context);
        });
    }

    static List<TypeManifest> nestedTypes(List<TypeManifest> events) {
        return Streams.concat(events.stream(), sealedSubtypes(events).stream())
                .flatMap(event -> event.fields().stream())
                .map(VariableManifest::type)
                .filter(type -> isNestedRecord(type) || isListOfNestedRecord(type))
                .map(type -> isListOfNestedRecord(type) ? type.parameters().getFirst() : type)
                .distinct()
                .toList();
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
        return !type.isStandardType() && !type.isEnum() && !isLogicalType(type);
    }

    static boolean isLogicalType(TypeManifest type) {
        return type.is(Instant.class)
                || type.is(LocalDate.class)
                || type.is(Duration.class)
                || type.is(Reference.class);
    }
}
