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
                .compile(sourceOf("event/avsc/simple/source/SimpleAvscEventMarker.java"));
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
    }

    @Test
    void nonPrimitiveAvscEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nonprimitive/source/NonPrimitiveAvscEventMarker.java"));
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
                .compile(sourceOf("event/avsc/nullable/source/NullableAvscEventMarker.java"));
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
                .compile(sourceOf("event/avsc/array/source/ArrayAvscEventMarker.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("List<String> tags");
    }

    @Test
    void simpleAvscEventGeneratesSchemaFactory() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvscEventMarker.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventSchemaFactory")
                .isNotNull();
    }
}
