package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class UnresolvedAnnotationTest {

    @Test
    void wrongImportYieldsJavacsOwnErrorInsteadOfAProcessorCrash() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/unresolvedannotation/source/Document.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot find symbol");
        assertThat(compilation.errors())
                .noneMatch(error -> error.getMessage(null).contains("IncompleteAnnotationException"))
                .noneMatch(error -> error.getMessage(null).contains("missing element"));
    }
}
