package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/** A plugin for generating Avro converters and schema factories for events annotated with {@link Event} with Avro serialization. */
public class AvroPlugin implements PrefabPlugin {
    private EventToGenericRecordConverterWriter toGenericRecordConverterWriter;
    private GenericRecordToEventConverterWriter toEventConverterWriter;
    private EventSchemaFactoryWriter eventSchemaFactoryWriter;
    private PrefabContext context;

    /** Constructs a new AvroPlugin. */
    public AvroPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        toGenericRecordConverterWriter = new EventToGenericRecordConverterWriter(context);
        toEventConverterWriter = new GenericRecordToEventConverterWriter(context);
        eventSchemaFactoryWriter = new EventSchemaFactoryWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        // Regular events: directly annotated with @Event(serialization = AVRO).
        // Contract interfaces (@Avsc) are excluded — their generated records are handled below.
        var directEvents = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(event -> Objects.requireNonNull(event.getAnnotation(Event.class)).serialization() == Event.Serialization.AVRO)
                .filter(event -> event.getAnnotation(Avsc.class) == null)
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();

        // AVSC-generated records: they carry @Event but may not surface reliably via
        // getElementsAnnotatedWith in the same round they are compiled.
        // Find them as root elements (newly compiled in this round) that implement an
        // @Avsc-annotated contract interface — this is reliable across all AP implementations.
        // The annotation check is done directly on the interface element (not via roundEnv) so it
        // works even when getElementsAnnotatedWith(Avsc.class) returns nothing in round 2.
        var avscEvents = context.roundEnvironment().getRootElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.RECORD)
                .map(e -> (TypeElement) e)
                .filter(r -> r.getInterfaces().stream()
                        .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                        .anyMatch(iface -> iface.getAnnotation(Avsc.class) != null))
                .map(r -> TypeManifest.of(r.asType(), context.processingEnvironment()))
                .toList();

        var events = Stream.concat(directEvents.stream(), avscEvents.stream())
                .distinct()
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
        var result = new ArrayList<TypeManifest>();
        var toProcess = new ArrayDeque<>(nestedTypes(events));
        result.addAll(toProcess);
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
