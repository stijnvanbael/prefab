package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void controllerValidatesTheFileConstraintsOfTheUpload() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/updatebinary/source/Document.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.updatebinary.infrastructure.http.DocumentController")
                .contentsAsUtf8String()
                .containsMatch("@ContentType\\([^)]*\"application/pdf\"[^)]*\\) @FileSize\\(max = 1024[^)]*\\) "
                        + "MultipartFile attachment");
    }

    @Test
    void testClientUploadsTheFileWithTheHttpMethodOfTheUpdate() {
        var processor = new SecurityGenerationTest.CapturingSecurityProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(sourceOf("rest/updatebinary/source/Document.java"));

        assertThat(compilation).succeeded();
        var client = processor.capturedSources.stream()
                .filter(source -> source.contains("class DocumentClient"))
                .findFirst()
                .orElseThrow();
        assertTrue(client.contains(
                "mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.valueOf(\"PUT\"), \"/documents/{id}/attachment\", id)"),
                client);
        assertFalse(client.contains("return mockMvc.perform(MockMvcRequestBuilders.multipart"), client);
    }
}
