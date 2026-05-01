package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .generatedSourceFile("rest.asynccreate.application.PlaceOrderRequest")
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
                .contains("void placeOrder(");
    }

    @Test
    void multipleAsyncCreateFactoriesGenerateOneMethodEach() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncmultiplecreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("placeOrder(");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("quickOrder(");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("void placeOrder(");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("void quickOrder(");
    }

    @Test
    void multipleAsyncCreateFactoriesGenerateOneRequestRecordEach() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncmultiplecreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.PlaceOrderRequest")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.QuickOrderRequest")
                .isNotNull();
    }

    @Test
    void multipleAsyncCreateFactoriesGenerateAllMethodsInTestClient() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncmultiplecreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "rest/asyncmultiplecreate/OrderClient.java")
                .contentsAsUtf8String()
                .contains("void placeOrder(");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "rest/asyncmultiplecreate/OrderClient.java")
                .contentsAsUtf8String()
                .contains("void quickOrder(");
    }

    @Test
    void multipleVoidAsyncCreateFactoriesWithPathVarsGenerateAllMethodsInTestClient() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncmultiplecreatewithpathvar/source/MeteringConfig.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT,
                        "rest/asyncmultiplecreatewithpathvar/MeteringConfigClient.java")
                .contentsAsUtf8String()
                .contains("void closeForInput(");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT,
                        "rest/asyncmultiplecreatewithpathvar/MeteringConfigClient.java")
                .contentsAsUtf8String()
                .contains("void openForInput(");
    }

    @Test
    void multipleVoidAsyncCreateFactoriesGenerateAllMethodsInTestClient() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncmultiplecreatenopathvar/source/MeteringConfig.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT,
                        "rest/asyncmultiplecreatenopathvar/MeteringConfigClient.java")
                .contentsAsUtf8String()
                .contains("void closeForInput(");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT,
                        "rest/asyncmultiplecreatenopathvar/MeteringConfigClient.java")
                .contentsAsUtf8String()
                .contains("void openForInput(");
        assertThat(compilation)
                .generatedSourceFile(
                        "rest.asyncmultiplecreatenopathvar.infrastructure.http.MeteringConfigController")
                .contentsAsUtf8String()
                .contains("closeForInput(");
        assertThat(compilation)
                .generatedSourceFile(
                        "rest.asyncmultiplecreatenopathvar.infrastructure.http.MeteringConfigController")
                .contentsAsUtf8String()
                .contains("openForInput(");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreatenopathvar.application.MeteringConfigService")
                .contentsAsUtf8String()
                .contains("void closeForInput(");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncmultiplecreatenopathvar.application.MeteringConfigService")
                .contentsAsUtf8String()
                .contains("void openForInput(");
    }

    @Test
    void asyncCreateWithParentAddsPathVariableToController() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/asynccreatewithparent/source/Project.java"),
                        sourceOf("rest/asynccreatewithparent/source/Task.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreatewithparent.infrastructure.http.TaskController")
                .contentsAsUtf8String()
                .contains("@PathVariable");
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreatewithparent.infrastructure.http.TaskController")
                .contentsAsUtf8String()
                .contains("String projectId");
    }

    @Test
    void asyncCreateWithParentPassesParentIdToService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/asynccreatewithparent/source/Project.java"),
                        sourceOf("rest/asynccreatewithparent/source/Task.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreatewithparent.application.TaskService")
                .contentsAsUtf8String()
                .contains("String projectId");
        assertThat(compilation)
                .generatedSourceFile("rest.asynccreatewithparent.application.TaskService")
                .contentsAsUtf8String()
                .contains("new Reference<>(projectId)");
    }

    @Test
    void asyncCreateWithDuplicateMappingFailsCompilation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncduplicatemapping/source/Order.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("same HTTP method and path");
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
    void asyncUpdateReturns404WhenAggregateNotFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncupdate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncupdate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("notFound()");
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

    @Test
    void asyncCreatePathVariableIsExcludedFromRequestRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncpathvariable/source/Ticket.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncpathvariable.application.OpenRequest")
                .contentsAsUtf8String()
                .doesNotContain("String queue");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncpathvariable.application.OpenRequest")
                .contentsAsUtf8String()
                .contains("String subject");
    }

    @Test
    void asyncCreatePathVariableIsPassedDirectlyToFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncpathvariable/source/Ticket.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncpathvariable.application.TicketService")
                .contentsAsUtf8String()
                .contains("String queue");
        assertThat(compilation)
                .generatedSourceFile("rest.asyncpathvariable.application.TicketService")
                .contentsAsUtf8String()
                .contains("Ticket.open(queue, request.subject())");
    }

    @Test
    void asyncUpdatePathVariableIsExcludedFromRequestRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncpathvariable/source/Ticket.java"));

        assertThat(compilation).succeeded();
        var unexpectedFiles = compilation.generatedSourceFiles().stream()
                .map(javax.tools.JavaFileObject::getName)
                .filter(name -> name.contains("TicketTransitionRequest"))
                .toList();
        assertTrue(unexpectedFiles.isEmpty(),
                () -> "Expected no files with 'TicketTransitionRequest' but found: " + unexpectedFiles);
    }

    @Test
    void asyncCreatePathVariableIsPassedFromControllerToService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncpathvariable/source/Ticket.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncpathvariable.infrastructure.http.TicketController")
                .contentsAsUtf8String()
                .contains("service.open(queue, request)");
    }

    @Test
    void asyncUpdatePathVariableIsPassedFromControllerToService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asyncpathvariable/source/Ticket.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.asyncpathvariable.infrastructure.http.TicketController")
                .contentsAsUtf8String()
                .contains("service.transition(id, status)");
    }

}
