package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class EventSchemaFactoryWriterTest {

    @Test
    void simpleEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.SimpleEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_SimpleEventSchemaFactory\")");
        assertThat(source).contains("Schema.createRecord(\"SimpleEvent\"");
        assertThat(source).contains("Schema.create(Schema.Type.STRING)");
        assertThat(source).contains("Schema.create(Schema.Type.INT)");
        assertThat(source).contains("Schema.create(Schema.Type.DOUBLE)");
        assertThat(source).contains("Schema.create(Schema.Type.BOOLEAN)");
        assertThat(source).contains("public Schema createSchema()");
    }

    @Test
    void inheritedFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.InheritEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_InheritEventSchemaFactory\")");
        // fields from both the supertype and the subtype appear in the schema
        assertThat(source).contains("\"superField\"");
        assertThat(source).contains("\"subField\"");
    }

    @Test
    void nonPrimitiveFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NonPrimitiveEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_NonPrimitiveEventSchemaFactory\")");
        // enum type is mapped to an Avro enum
        assertThat(source).contains("Schema.createEnum(");
        // temporal types use logical type helpers
        assertThat(source).contains("LogicalTypes.timestampMillis()");
        assertThat(source).contains("LogicalTypes.date()");
    }

    @Test
    void nullableFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NullableEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_NullableEventSchemaFactory\")");
        // nullable fields use the union helper
        assertThat(source).contains("SchemaSupport.createNullableSchema(");
    }

    @Test
    void nestedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        var topLevel = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NestedRecordEventSchemaFactory");
        assertThat(topLevel).contains("@Component(\"event_avro_NestedRecordEventSchemaFactory\")");
        // nested record schema is injected as a constructor dependency
        assertThat(topLevel).contains("NestedRecordEventMoneySchemaFactory nestedRecordEventMoneySchemaFactory");
        assertThat(topLevel).contains("nestedRecordEventMoneySchemaFactory.createSchema()");

        var nested = generatedSourceOf(compilation, "event.avro.infrastructure.avro.NestedRecordEventMoneySchemaFactory");
        assertThat(nested).contains("@Component(\"event_avro_NestedRecordEventMoneySchemaFactory\")");
        assertThat(nested).contains("Schema.createRecord(\"NestedRecordEvent_Money\"");
    }

    @Test
    void arrayField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.ArrayFieldEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_ArrayFieldEventSchemaFactory\")");
        // array fields use Schema.createArray
        assertThat(source).contains("Schema.createArray(");
    }

    @Test
    void sealedHierarchy() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.HierarchyEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_HierarchyEventSchemaFactory\")");
        // union of sub-type schemas is created from injected sub-schema factories
        assertThat(source).contains("Schema.createUnion(");
        assertThat(source).contains("HierarchyEventCreatedSchemaFactory");
        assertThat(source).contains("HierarchyEventUpdatedSchemaFactory");
    }

    @Test
    void exampleField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/examplefield/source/ExampleFieldEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.ExampleFieldEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_ExampleFieldEventSchemaFactory\")");
        // @Example value is applied via withSample helper
        assertThat(source).contains("SchemaSupport.withSample(");
        assertThat(source).contains("\"john-doe\"");
    }

    @Test
    void docField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/docfield/source/DocFieldEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.DocFieldEventSchemaFactory");
        assertThat(source).contains("@Component(\"event_avro_DocFieldEventSchemaFactory\")");
        // @Doc value is passed as the doc argument of Schema.Field
        assertThat(source).contains("\"The full name of the person\"");
    }

    @Test
    void nullableMetadataUsesLatestCompilationSnapshot() {
        var baseSource = JavaFileObjects.forSourceString("event.avro.NullableEvent", """
                package event.avro;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "nullable", serialization = Event.Serialization.AVRO)
                public record NullableEvent(String id, String name, String description) {}
                """);
        var nullableSource = JavaFileObjects.forSourceString("event.avro.NullableEvent", """
                package event.avro;
                import be.appify.prefab.core.annotations.Event;
                import jakarta.annotation.Nullable;
                @Event(topic = "nullable", serialization = Event.Serialization.AVRO)
                public record NullableEvent(String id, String name, @Nullable String description) {}
                """);

        var firstCompilation = javac().withProcessors(new PrefabProcessor()).compile(baseSource);
        assertThat(firstCompilation).succeeded();
        assertThat(generatedSourceOf(firstCompilation, "event.avro.infrastructure.avro.NullableEventSchemaFactory"))
                .doesNotContain("SchemaSupport.createNullableSchema");

        var secondCompilation = javac().withProcessors(new PrefabProcessor()).compile(nullableSource);
        assertThat(secondCompilation).succeeded();
        assertThat(generatedSourceOf(secondCompilation, "event.avro.infrastructure.avro.NullableEventSchemaFactory"))
                .contains("SchemaSupport.createNullableSchema");
    }

    @Test
    void avscBackedNestedRecordLoadsTopLevelSchemaFromAvscFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedavsc/source/NestedAvscContract.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation)
                .generatedSourceFile("event.avro.infrastructure.avro.NestedAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("loadExpectedSchema()");

        assertThat(compilation)
                .generatedSourceFile("event.avro.infrastructure.avro.AddressSchemaFactory")
                .contentsAsUtf8String()
                .doesNotContain("loadExpectedSchema()");
    }
}
