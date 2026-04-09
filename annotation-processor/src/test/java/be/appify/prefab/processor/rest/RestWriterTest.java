package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class RestWriterTest {

    @Test
    void requestValidationAnnotationsAreGenerated() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/validation/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.validation.application.CreateProductRequest")
                .contentsAsUtf8String()
                .contains("@NotNull");
        assertThat(compilation)
                .generatedSourceFile("rest.validation.application.CreateProductRequest")
                .contentsAsUtf8String()
                .contains("@Size(max = 255)");
    }

    @Test
    void nonStringValueTypeParametersUseInnerFieldType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/valuetype/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.valuetype.application.CreateProductRequest")
                .contentsAsUtf8String()
                .contains("String name");
        assertThat(compilation)
                .generatedSourceFile("rest.valuetype.application.CreateProductRequest")
                .contentsAsUtf8String()
                .contains("BigDecimal price");
    }
}
