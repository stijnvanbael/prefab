package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class CreateBinaryTest {

    @Test
    void controllerValidatesTheFileConstraintsOfTheUpload() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/createbinary/source/Document.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.createbinary.infrastructure.http.DocumentController")
                .contentsAsUtf8String()
                .containsMatch("@ContentType\\([^)]*\"application/pdf\"[^)]*\\) @FileSize\\(max = 1024[^)]*\\) "
                        + "MultipartFile file");
    }

    @Test
    void controllerReadsTheOtherFieldsFromTheBodyPart() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/createbinary/source/Document.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.createbinary.infrastructure.http.DocumentController")
                .contentsAsUtf8String()
                .contains("@RequestPart(name = \"body\") CreateDocumentRequest request");
    }
}
