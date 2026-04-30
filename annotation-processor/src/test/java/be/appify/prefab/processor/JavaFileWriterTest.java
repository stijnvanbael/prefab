package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class JavaFileWriterTest {

    @Test
    void noteEmittedWhenGenerationSkippedDueToExistingSourceFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/override/source/Widget.java"),
                        sourceOf("rest/override/source/application/WidgetService.java")
                );

        assertThat(compilation).hadNoteContaining("rest.override.application.WidgetService");
        assertThat(compilation).hadNoteContaining("already exists");
    }

    @Test
    void noNoteEmittedWhenFileAlreadyGeneratedByProcessor() {
        var compilation = javac()
                .withProcessors(new DoubleWriteProcessor())
                .compile(sourceOf("rest/override/source/Widget.java"));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).hadNoteCount(0);
    }

    /**
     * A processor that intentionally writes the same file twice to simulate the case
     * where the annotation processor tries to regenerate a file it already produced.
     */
    @SupportedAnnotationTypes("be.appify.prefab.core.annotations.Aggregate")
    @SupportedSourceVersion(SourceVersion.RELEASE_21)
    static class DoubleWriteProcessor extends AbstractProcessor {

        private boolean alreadyWritten = false;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!alreadyWritten) {
                alreadyWritten = true;
                var writer = new JavaFileWriter(processingEnv, "application");
                var type = TypeSpec.classBuilder("GeneratedService").build();
                writer.writeFile("rest.override", "GeneratedService", type);
                writer.writeFile("rest.override", "GeneratedService", type);
            }
            return false;
        }
    }
}
