package be.appify.prefab.avro.processor.asyncapi;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AvscEventSchemaDocumentationTest {

    @Test
    void avscEventGeneratesAsyncApiDocumentation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .contains("\"SimpleAvscEvent\"");
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .contains("\"name\"");
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .contains("\"age\"");
    }
}
