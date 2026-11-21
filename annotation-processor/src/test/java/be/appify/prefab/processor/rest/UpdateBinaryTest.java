package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class UpdateBinaryTest {

    @Test
    void updateWithBinaryParameterStoresTheUploadedFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/updatebinary/source/Document.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.updatebinary.application.DocumentService")
                .contentsAsUtf8String()
                .contains("aggregate.replaceAttachment(storageService.store(request.attachment()))");
    }
}
