package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AsyncCommitWriterTest {

    @Test
    void asyncCreateGenerates202AndCallsFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asynccreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("accepted()");
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreate.application.OrderService")
                .contentsAsUtf8String()
                .doesNotContain("DomainEventPublisher");
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("placeOrder");
    }

    @Test
    void asyncCreateGeneratesRequestRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asynccreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreate.application.CreateOrderRequest")
                .isNotNull();
    }

    @Test
    void asyncCreateServiceReturnsVoid() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asynccreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("void create(");
    }

    @Test
    void asyncUpdateGenerates202AndDoesNotSave() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncupdate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncupdate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("accepted()");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncupdate.application.OrderService")
                .contentsAsUtf8String()
                .doesNotContain("orderRepository.save");
    }


    @Test
    void methodLevelAsyncCommitOnCreateGenerates202() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncmethodlevel/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmethodlevel.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("accepted()");
    }
}
