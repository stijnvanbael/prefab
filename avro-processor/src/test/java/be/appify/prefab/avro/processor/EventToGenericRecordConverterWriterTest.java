package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class EventToGenericRecordConverterWriterTest {

    @Test
    void simpleEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.SimpleEventToGenericRecordConverter");
        assertThat(source).contains("@Component(\"event_avro_SimpleEventToGenericRecordConverter\")");
        assertThat(source).contains("implements Converter<SimpleEvent, GenericRecord>");
        assertThat(source).contains("genericRecord.put(\"name\", event.name())");
        assertThat(source).contains("genericRecord.put(\"age\", event.age())");
    }

    @Test
    void inheritedFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.InheritEventToGenericRecordConverter");
        assertThat(source).contains("@Component(\"event_avro_InheritEventToGenericRecordConverter\")");
        // fields from both super and sub type are put in the record
        assertThat(source).contains("genericRecord.put(\"superField\", event.superField())");
        assertThat(source).contains("genericRecord.put(\"subField\", event.subField())");
    }

    @Test
    void nonPrimitiveFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NonPrimitiveEventToGenericRecordConverter");
        assertThat(source).contains("@Component(\"event_avro_NonPrimitiveEventToGenericRecordConverter\")");
        // enum is converted to a GenericData.EnumSymbol
        assertThat(source).contains("GenericData.EnumSymbol");
        // temporal types use explicit conversions
        assertThat(source).contains("toEpochMilli()");
        assertThat(source).contains("toEpochDay()");
    }

    @Test
    void nullableFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NullableEventToGenericRecordConverter");
        assertThat(source).contains("@Component(\"event_avro_NullableEventToGenericRecordConverter\")");
        // nullable field is put as-is (null is allowed)
        assertThat(source).contains("genericRecord.put(\"description\", event.description())");
    }

    @Test
    void nestedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        var topLevel = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NestedRecordEventToGenericRecordConverter");
        assertThat(topLevel).contains("@Component(\"event_avro_NestedRecordEventToGenericRecordConverter\")");
        // nested record is converted using its own converter, injected as dependency
        assertThat(topLevel).contains("NestedRecordEventMoneyToGenericRecordConverter nestedRecordEventMoneyToGenericRecordConverter");
        assertThat(topLevel).contains("nestedRecordEventMoneyToGenericRecordConverter.convert(");

        var nested = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NestedRecordEventMoneyToGenericRecordConverter");
        assertThat(nested).contains("@Component(\"event_avro_NestedRecordEventMoneyToGenericRecordConverter\")");
        assertThat(nested).contains("implements Converter<NestedRecordEvent.Money, GenericRecord>");
    }

    @Test
    void arrayField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.ArrayFieldEventToGenericRecordConverter");
        assertThat(source).contains("@Component(\"event_avro_ArrayFieldEventToGenericRecordConverter\")");
        // array fields use GenericData.Array with the array schema
        assertThat(source).contains("GenericData.Array");
        assertThat(source).contains("SchemaSupport.arraySchemaOf(");
    }

    @Test
    void sealedHierarchy() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();

        var union = generatedSourceOf(compilation, "event.avro.infrastructure.avro.HierarchyEventToGenericRecordConverter");
        assertThat(union).contains("@Component(\"event_avro_HierarchyEventToGenericRecordConverter\")");
        // union converter dispatches via switch on sealed subtype
        assertThat(union).contains("case HierarchyEvent.Created v -> hierarchyEventCreatedToGenericRecordConverter.convert(v)");
        assertThat(union).contains("case HierarchyEvent.Updated v -> hierarchyEventUpdatedToGenericRecordConverter.convert(v)");

        var created = generatedSourceOf(compilation, "event.avro.infrastructure.avro.HierarchyEventCreatedToGenericRecordConverter");
        assertThat(created).contains("@Component(\"event_avro_HierarchyEventCreatedToGenericRecordConverter\")");
        assertThat(created).contains("implements Converter<HierarchyEvent.Created, GenericRecord>");

        var updated = generatedSourceOf(compilation, "event.avro.infrastructure.avro.HierarchyEventUpdatedToGenericRecordConverter");
        assertThat(updated).contains("@Component(\"event_avro_HierarchyEventUpdatedToGenericRecordConverter\")");
        assertThat(updated).contains("implements Converter<HierarchyEvent.Updated, GenericRecord>");
    }
}
