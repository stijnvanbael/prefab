package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class EventEvolutionTest {

    @Test
    void schemaCarriesDefaultsAndAliases() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/evolution/source/EvolvedEvent.java"));
        assertThat(compilation).succeeded();

        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.EvolvedEventSchemaFactory");

        assertThat(source).contains("SchemaSupport.withAliases(Schema.createRecord(\"EvolvedEvent\"");
        assertThat(source).contains("\"LegacyEvolved\", \"legacy.OldEvolved\")");
        assertThat(source).doesNotContain("Not.Valid$Name");
        assertThat(source).contains("SchemaSupport.withAliases(new Schema.Field(\"name\"");
        assertThat(source).contains("\"fullName\", \"title\")");
        assertThat(source).contains("new Schema.Field(\"label\", Schema.create(Schema.Type.STRING), null, \"unknown\")");
        assertThat(source).contains("new Schema.Field(\"count\", Schema.create(Schema.Type.INT), null, 7)");
        assertThat(source).contains("new Schema.Field(\"total\", Schema.create(Schema.Type.LONG), null, 2L)");
        assertThat(source).contains("new Schema.Field(\"ratio\", Schema.create(Schema.Type.DOUBLE), null, 1.5D)");
        assertThat(source).contains("new Schema.Field(\"factor\", Schema.create(Schema.Type.FLOAT), null, 0.5F)");
        assertThat(source).contains("new Schema.Field(\"active\", Schema.create(Schema.Type.BOOLEAN), null, true)");
        assertThat(source).contains("null, \"PENDING\")");
        assertThat(source).contains("null, List.of())");
    }

    @Test
    void readerSchemaIsRegisteredForTheEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/evolution/source/EvolvedEvent.java"));
        assertThat(compilation).succeeded();

        var source = generatedSourceOf(compilation, "event.avro.infrastructure.avro.EvolvedEventReaderSchemaRegistrar");

        assertThat(source).contains("implements EventRegistryCustomizer");
        assertThat(source).contains("registry.registerReaderSchema(EvolvedEvent.class, schemaFactory.createSchema())");
    }

    @Test
    void invalidDefaultLiteralIsRejected() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/invaliddefault/source/InvalidDefaultEvent.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Invalid @Default on field 'count'");
    }

    @Test
    void defaultOnNullableComponentIsRejected() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullabledefault/source/NullableDefaultEvent.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("already defaults to null");
    }

    @Test
    void previousNamesCollidingWithCurrentNamesAreRejected() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/previousnamecollision/source/CollisionEvent.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("collides with the current field 'age'");
        assertThat(compilation).hadErrorContaining("also declared as previous name of another field");
    }

    @Test
    void readerSchemaIsRegisteredForAvscGeneratedEvents() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/defaults/source/DefaultsAvsc.java"));
        assertThat(compilation).succeeded();

        var source = generatedSourceOf(compilation,
                "event.avsc.defaults.infrastructure.avro.DefaultsAvscEventReaderSchemaRegistrar");

        assertThat(source).contains("registry.registerReaderSchema(DefaultsAvscEvent.class, schemaFactory.createSchema())");
    }}
