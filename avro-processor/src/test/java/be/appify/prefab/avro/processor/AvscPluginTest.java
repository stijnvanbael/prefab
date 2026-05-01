package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AvscPluginTest {

    @Test
    void simpleAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("@Event(");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("topic = \"simple-avsc\"");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("serialization = Event.Serialization.AVRO");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("String name");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("int age");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("double score");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("boolean active");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("implements SimpleAvsc");
    }

    @Test
    void nonPrimitiveAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nonprimitive/source/NonPrimitiveAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.NonPrimitiveAvscEvent")
                .contentsAsUtf8String()
                .contains("Instant timestamp");
        assertThat(compilation).generatedSourceFile("event.avsc.NonPrimitiveAvscEvent")
                .contentsAsUtf8String()
                .contains("LocalDate date");
        assertThat(compilation).generatedSourceFile("event.avsc.NonPrimitiveAvscEvent")
                .contentsAsUtf8String()
                .contains("Duration duration");
    }

    @Test
    void nullableAvscEventSchemaFactoryGeneratesNullableUnion() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullable/source/NullableAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avsc/nullable/expected/NullableAvscEventSchemaFactory.java"));
    }

    @Test
    void nullableAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullable/source/NullableAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("String name");
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("@Nullable");
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("String description");
    }

    @Test
    void arrayAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/array/source/ArrayAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("List<String> tags");
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("implements ArrayAvsc");
    }

    @Test
    void interfaceNameCollidingWithRecordNameReportsError() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/array/source/ArrayAvscEvent.java"));
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("conflicts with the contract interface name");
    }

    @Test
    void simpleAvscEventGeneratesSchemaFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avsc/simple/expected/SimpleAvscEventSchemaFactory.java"));
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.GenericRecordToSimpleAvscEventConverter")
                .isNotNull();
    }

    @Test
    void multiPathAvscSchemaFactoriesLoadFromCorrectAvscFiles() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/multi/source/MultiAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.multi.infrastructure.avro.MultiAvscEventASchemaFactory")
                .contentsAsUtf8String()
                .contains("event/avsc/multi/source/MultiAvscEventA.avsc");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.multi.infrastructure.avro.MultiAvscEventBSchemaFactory")
                .contentsAsUtf8String()
                .contains("event/avsc/multi/source/MultiAvscEventB.avsc");
    }

    @Test
    void multiPathAvscGeneratesOneRecordPerSchema() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/multi/source/MultiAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventA")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventA")
                .contentsAsUtf8String()
                .contains("double amount");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventA")
                .contentsAsUtf8String()
                .contains("implements MultiAvsc");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventB")
                .contentsAsUtf8String()
                .contains("String reference");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventB")
                .contentsAsUtf8String()
                .contains("int count");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventB")
                .contentsAsUtf8String()
                .contains("implements MultiAvsc");
    }

    @Test
    void sealedInterfaceWithPermitsReferencingGeneratedTypesCompiles() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/sealed/source/SealedMultiAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.sealed.SealedMultiAvscEventA")
                .contentsAsUtf8String()
                .contains("implements SealedMultiAvsc");
        assertThat(compilation).generatedSourceFile("event.avsc.sealed.SealedMultiAvscEventB")
                .contentsAsUtf8String()
                .contains("implements SealedMultiAvsc");
    }

    @Test
    void generatedAvscEventRecordContainsNestedBuilder() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("public static final class Builder");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("public static Builder builder()");
    }
}
