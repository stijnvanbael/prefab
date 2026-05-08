package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Avsc;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** A plugin for generating Avro converters and schema factories for events annotated with {@link Event} with Avro serialization. */
public class AvroPlugin implements PrefabPlugin {
    private EventToGenericRecordConverterWriter toGenericRecordConverterWriter;
    private GenericRecordToEventConverterWriter toEventConverterWriter;
    private EventSchemaFactoryWriter eventSchemaFactoryWriter;
    private PrefabContext context;
    private final Set<String> writtenTypeNames = new LinkedHashSet<>();
    private final Set<String> writtenToEventOnlyTypeNames = new LinkedHashSet<>();


    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        toGenericRecordConverterWriter = new EventToGenericRecordConverterWriter(context);
        toEventConverterWriter = new GenericRecordToEventConverterWriter(context);
        eventSchemaFactoryWriter = new EventSchemaFactoryWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        // For additional Avro infrastructure, only generate for event types owned by the current
        // compilation unit. Dependency events may already ship their own generated artifacts.
        var events = context.eventElementsFromCurrentCompilation()
                .filter(e -> EventPlatformPluginSupport.isAvscGeneratedRecord(e)
                        || Objects.requireNonNull(e.getAnnotation(Event.class)).serialization() == Event.Serialization.AVRO)
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();

        events.forEach(this::writeConvertersIfNotWritten);
        allNestedTypes(events).forEach(this::writeConvertersIfNotWritten);
        sealedSubtypes(events).forEach(this::writeConvertersIfNotWritten);
        var supertypes = avroEventSupertypes(events);
        supertypes.stream()
                .filter(AvroPlugin::isAvscContract)
                .forEach(this::writeConvertersIfNotWritten);
        supertypes.stream()
                .filter(type -> !isAvscContract(type))
                .forEach(this::writeToEventConverterIfNotWritten);
    }

    private void writeConvertersIfNotWritten(TypeManifest type) {
        var key = type.packageName() + "." + type.simpleName();
        if (writtenTypeNames.contains(key)) {
            return;
        }
        var wroteAll = toGenericRecordConverterWriter.writeConverter(type)
                && toEventConverterWriter.writeConverter(type)
                && eventSchemaFactoryWriter.writeSchemaFactory(type);
        if (wroteAll) {
            writtenTypeNames.add(key);
        }
    }

    private void writeToEventConverterIfNotWritten(TypeManifest type) {
        var key = type.packageName() + "." + type.simpleName();
        if (writtenTypeNames.contains(key) || writtenToEventOnlyTypeNames.contains(key)) {
            return;
        }
        if (toEventConverterWriter.writeConverter(type)) {
            writtenToEventOnlyTypeNames.add(key);
        }
    }

    private static List<TypeManifest> avroEventSupertypes(List<TypeManifest> events) {
        return events.stream()
                .map(event -> event.supertypeWithAnnotation(Event.class))
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    private static boolean isAvscContract(TypeManifest type) {
        return type.asElement() != null && type.asElement().getAnnotation(Avsc.class) != null;
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
        // LinkedHashSet preserves insertion order and provides O(1) contains() checks,
        // replacing the previous ArrayList that caused O(n^2) deduplication behaviour.
        var result = new LinkedHashSet<>(toProcess);
        while (!toProcess.isEmpty()) {
            var type = toProcess.poll();
            // Pass only the single type and its sealed subtypes; avoids re-expanding all
            // previously processed types' sealed subtypes on every BFS iteration.
            var combined = Stream.concat(Stream.of(type), type.permittedSubtypes().stream()).toList();
            combined.stream()
                    .flatMap(t -> t.fields().stream())
                    .map(VariableManifest::type)
                    .filter(fieldType -> isNestedRecord(fieldType) || isListOfNestedRecord(fieldType))
                    .map(fieldType -> isListOfNestedRecord(fieldType) ? fieldType.parameters().getFirst() : fieldType)
                    .filter(result::add)
                    .forEach(toProcess::add);
        }
        return List.copyOf(result);
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
