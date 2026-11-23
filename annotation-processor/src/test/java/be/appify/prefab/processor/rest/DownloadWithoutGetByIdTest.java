package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DownloadWithoutGetByIdTest {

    @Test
    void downloadWithoutGetByIdFailsWithAnErrorNamingTheMissingAnnotation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/downloadwithoutgetbyid/source/Document.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("[@Download] field 'attachment' on Document requires the aggregate "
                + "to be annotated with @GetById");
    }
}
