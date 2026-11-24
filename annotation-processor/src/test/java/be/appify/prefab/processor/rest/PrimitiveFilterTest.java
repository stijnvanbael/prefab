package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class PrimitiveFilterTest {

    @Test
    void filterOnPrimitiveFieldsIsExposedAsBoxedQueryParameters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/primitivefilter/source/Task.java"));

        assertThat(compilation).succeeded();
        var controller = assertThat(compilation)
                .generatedSourceFile("rest.primitivefilter.infrastructure.http.TaskController")
                .contentsAsUtf8String();
        controller.contains("Boolean done");
        controller.contains("Integer priority");
    }
}
