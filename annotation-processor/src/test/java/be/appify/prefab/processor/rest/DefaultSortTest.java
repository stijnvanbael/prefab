package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSortTest {

    @Test
    void controllerDeclaresDefaultSortOnPageableParameter() throws Exception {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/defaultsort/source/Note.java"));

        assertThat(compilation).succeeded();
        var controller = compilation.generatedSourceFile("rest.defaultsort.infrastructure.http.NoteController")
                .orElseThrow()
                .getCharContent(true)
                .toString();
        assertTrue(controller.contains("@SortDefault.SortDefaults("), controller);
        assertTrue(controller.contains("@SortDefault(sort = \"author\", direction = Sort.Direction.ASC)"), controller);
        assertTrue(controller.contains("@SortDefault(sort = \"title\", direction = Sort.Direction.DESC)"), controller);
    }

    @Test
    void unknownDefaultSortFieldIsRejected() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/defaultsort/invalid/source/Note.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("defaultSort: unknown field 'missing'");
    }
}
