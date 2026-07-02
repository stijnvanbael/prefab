package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class GenericRecordToEventConverterWriterTest {

    @Test
    void simpleEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToSimpleEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToSimpleEventConverter\")");
        assertThat(source).contains("implements Converter<GenericRecord, SimpleEvent>");
        assertThat(source).contains("SchemaSupport.getString(genericRecord, \"name\")");
    }

    @Test
    void inheritedFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToInheritEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToInheritEventConverter\")");
        // both inherited and own fields are read
        assertThat(source).contains("SchemaSupport.getString(genericRecord, \"superField\")");
        assertThat(source).contains("SchemaSupport.getString(genericRecord, \"subField\")");

        // supertype converter is also generated
        var superSource = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToSuperTypeConverter");
        assertThat(superSource).contains("@Component(\"event_avro_GenericRecordToSuperTypeConverter\")");
        assertThat(superSource).contains("implements Converter<GenericRecord, SuperType>");
    }

    @Test
    void interfaceSupertype() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/interfacesupertype/source/UserEvent.java"),
                        sourceOf("event/avro/interfacesupertype/source/UserCreated.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToUserEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToUserEventConverter\")");
        assertThat(source).contains("implements Converter<GenericRecord, UserEvent>");
        // dispatch via schema name for interface supertypes
        assertThat(source).contains("genericRecord.getSchema().getName()");
        assertThat(source).contains("genericRecordToUserCreatedConverter.convert(genericRecord)");
    }

    @Test
    void interfaceSupertypeWithAvroSchemaNameOverride() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/schemanameoverride/source/OverriddenUserEvent.java"),
                        sourceOf("event/avro/schemanameoverride/source/UserCreatedWithCustomSchema.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToOverriddenUserEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToOverriddenUserEventConverter\")");
        // switch uses the custom schema name, not the class name
        assertThat(source).contains("case \"UserCreatedV1\"");
    }

    @Test
    void nonPrimitiveFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToNonPrimitiveEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToNonPrimitiveEventConverter\")");
        // enum is read from the Avro record and converted via valueOf
        assertThat(source).contains("NonPrimitiveEvent.Status");
        // temporal types use SchemaSupport helpers
        assertThat(source).contains("SchemaSupport.get");
    }

    @Test
    void nullableFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToNullableEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToNullableEventConverter\")");
        // nullable fields also use SchemaSupport.getString (returns null if absent)
        assertThat(source).contains("SchemaSupport.getString(genericRecord, \"description\")");
    }

    @Test
    void nestedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        var topLevel = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToNestedRecordEventConverter");
        assertThat(topLevel).contains("@Component(\"event_avro_GenericRecordToNestedRecordEventConverter\")");
        // nested records are extracted via SchemaSupport.getRecord and delegated to child converter
        assertThat(topLevel).contains("SchemaSupport.getRecord(genericRecord, \"totalAmount\"");
        assertThat(topLevel).contains("genericRecordToNestedRecordEventMoneyConverter::convert");

        var nested = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToNestedRecordEventMoneyConverter");
        assertThat(nested).contains("@Component(\"event_avro_GenericRecordToNestedRecordEventMoneyConverter\")");
        assertThat(nested).contains("implements Converter<GenericRecord, NestedRecordEvent.Money>");
    }

    @Test
    void arrayField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToArrayFieldEventConverter");
        assertThat(source).contains("@Component(\"event_avro_GenericRecordToArrayFieldEventConverter\")");
        // array fields use SchemaSupport.getArray
        assertThat(source).contains("SchemaSupport.getArray(genericRecord, \"tags\"");
        assertThat(source).contains("SchemaSupport.getArray(genericRecord, \"lines\"");
    }

    @Test
    void sealedHierarchy() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();

        var union = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToHierarchyEventConverter");
        assertThat(union).contains("@Component(\"event_avro_GenericRecordToHierarchyEventConverter\")");
        // dispatches based on schema name to typed sub-converters
        assertThat(union).contains("case \"HierarchyEvent_Created\" -> genericRecordToHierarchyEventCreatedConverter.convert(genericRecord)");
        assertThat(union).contains("case \"HierarchyEvent_Updated\" -> genericRecordToHierarchyEventUpdatedConverter.convert(genericRecord)");

        var created = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToHierarchyEventCreatedConverter");
        assertThat(created).contains("@Component(\"event_avro_GenericRecordToHierarchyEventCreatedConverter\")");
        assertThat(created).contains("implements Converter<GenericRecord, HierarchyEvent.Created>");

        var updated = generatedSourceOf(compilation, "event.avro.infrastructure.avro.GenericRecordToHierarchyEventUpdatedConverter");
        assertThat(updated).contains("@Component(\"event_avro_GenericRecordToHierarchyEventUpdatedConverter\")");
        assertThat(updated).contains("implements Converter<GenericRecord, HierarchyEvent.Updated>");
    }
}
