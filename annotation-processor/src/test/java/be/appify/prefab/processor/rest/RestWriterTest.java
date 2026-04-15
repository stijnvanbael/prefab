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

    @Test
    void aggregateWithCreateAndUpdateGeneratesRequestRecords() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/testclient/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.testclient.application.CreatePersonRequest")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("rest.testclient.application.PersonUpdateRequest")
                .isNotNull();
    }

    @Test
    void aggregateReferencingGeneratedEventTypeCompiles() throws IOException {
        // Order has a field of type OrderCreatedEvent, which is generated from @Avsc in round 1.
        // This forces Order to be deferred to round 2.  The test verifies:
        //   (a) Compilation succeeds (processor handles round 2 correctly), and
        //   (b) The request record and controller are generated (JavaFileWriter output).
        // The test client (TestJavaFileWriter output) is written to disk during a real build;
        // the fix in TestJavaFileWriter.getRootPath() ensures it is generated in round 2 by
        // using the aggregate's own TypeElement instead of round.getRootElements().
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/withgeneratedevent/source/OrderEvents.java"),
                        sourceOf("rest/withgeneratedevent/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.withgeneratedevent.application.CreateOrderRequest")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("rest.withgeneratedevent.infrastructure.http.OrderController")
                .isNotNull();
    }
}
