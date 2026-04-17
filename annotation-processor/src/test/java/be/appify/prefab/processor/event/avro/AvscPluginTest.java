package be.appify.prefab.processor.event.avro;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AvscPluginTest {

    @Test
    void simpleAvscEvent() throws IOException {
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
    void nonPrimitiveAvscEvent() throws IOException {
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
    void nullableAvscEvent() throws IOException {
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
    void arrayAvscEvent() throws IOException {
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
    void interfaceNameCollidingWithRecordNameReportsError() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/array/source/ArrayAvscEvent.java"));
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("conflicts with the contract interface name");
    }

    @Test
    void simpleAvscEventGeneratesSchemaFactory() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventSchemaFactory")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.GenericRecordToSimpleAvscEventConverter")
                .isNotNull();
    }

    @Test
    void multiPathAvscGeneratesOneRecordPerSchema() throws IOException {
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
    void docPropertyInAvscEmitsDocAnnotationOnField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/docfield/source/DocFieldAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.DocFieldAvscEvent")
                .contentsAsUtf8String()
                .contains("@Doc(\"Full name of the person\")");
        assertThat(compilation).generatedSourceFile("event.avsc.DocFieldAvscEvent")
                .contentsAsUtf8String()
                .contains("int age");
    }
}
