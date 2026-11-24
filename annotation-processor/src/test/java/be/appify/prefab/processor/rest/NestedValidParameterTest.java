package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class NestedValidParameterTest {

    private static final String VALID_ANNOTATION = "@Valid";

    @Test
    void validOnRecordParametersDoesNotDuplicateTheAnnotation() throws Exception {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/nestedvalid/source/Address.java"),
                        sourceOf("rest/nestedvalid/source/Order.java"));

        assertThat(compilation).succeeded();
        for (var request : new String[] {"CreateOrderRequest", "OrderShipRequest"}) {
            var source = compilation.generatedSourceFile("rest.nestedvalid.application." + request)
                    .orElseThrow()
                    .getCharContent(false)
                    .toString();
            assertThat(source.split(VALID_ANNOTATION, -1))
                    .as("exactly one %s on the nested record in %s", VALID_ANNOTATION, request)
                    .hasSize(2);
        }
    }
}
