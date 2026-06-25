package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncCommitWriterTest {

    public static final Compilation asyncCreateOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/asynccreate/source/Order.java"));
    public static final Compilation asyncMultipleCreateOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/asyncmultiplecreate/source/Order.java"));
    public static final Compilation asyncCreateWithParentCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("rest/asynccreatewithparent/source/Project.java"),
                    sourceOf("rest/asynccreatewithparent/source/Task.java"));
    public static final Compilation asyncUpdateOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/asyncupdate/source/Order.java"));
    public static final Compilation asyncPathVariableTicketCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/asyncpathvariable/source/Ticket.java"));

    @Test
    void asyncCreateGenerates202AndCallsFactory() {
        assertThat(asyncCreateOrderCompilation).succeeded();
        assertThat(asyncCreateOrderCompilation)
                .generatedSourceFile("rest.asynccreate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("accepted()");
        assertThat(asyncCreateOrderCompilation)
                .generatedSourceFile("rest.asynccreate.application.OrderService")
                .contentsAsUtf8String()
                .doesNotContain("DomainEventPublisher");
        assertThat(asyncCreateOrderCompilation)
                .generatedSourceFile("rest.asynccreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("placeOrder");
    }

    @Test
    void asyncCreateGeneratesRequestRecord() {
        assertThat(asyncCreateOrderCompilation).succeeded();
        assertThat(asyncCreateOrderCompilation)
                .generatedSourceFile("rest.asynccreate.application.PlaceOrderRequest")
                .isNotNull();
    }

    @Test
    void asyncCreateServiceReturnsVoid() {
        assertThat(asyncCreateOrderCompilation).succeeded();
        assertThat(asyncCreateOrderCompilation)
                .generatedSourceFile("rest.asynccreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("void placeOrder(");
    }

    @Test
    void multipleAsyncCreateFactoriesGenerateOneMethodEach() {
        assertThat(asyncMultipleCreateOrderCompilation).succeeded();
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedSourceFile("rest.asyncmultiplecreate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("placeOrder(");
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedSourceFile("rest.asyncmultiplecreate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("quickOrder(");
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("void placeOrder(");
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.OrderService")
                .contentsAsUtf8String()
                .contains("void quickOrder(");
    }

    @Test
    void multipleAsyncCreateFactoriesGenerateOneRequestRecordEach() {
        assertThat(asyncMultipleCreateOrderCompilation).succeeded();
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.PlaceOrderRequest")
                .isNotNull();
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedSourceFile("rest.asyncmultiplecreate.application.QuickOrderRequest")
                .isNotNull();
    }

    @Test
    void multipleAsyncCreateFactoriesGenerateAllMethodsInTestClient() {
        assertThat(asyncMultipleCreateOrderCompilation).succeeded();
        assertThat(asyncMultipleCreateOrderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "rest/asyncmultiplecreate/OrderClient.java")
                .contentsAsUtf8String()
                .contains("void placeOrder(");
        assertThat(asyncMultipleCreateOrderCompilation)
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
        assertThat(asyncCreateWithParentCompilation).succeeded();
        assertThat(asyncCreateWithParentCompilation)
                .generatedSourceFile("rest.asynccreatewithparent.infrastructure.http.TaskController")
                .contentsAsUtf8String()
                .contains("@PathVariable");
        assertThat(asyncCreateWithParentCompilation)
                .generatedSourceFile("rest.asynccreatewithparent.infrastructure.http.TaskController")
                .contentsAsUtf8String()
                .contains("String projectId");
    }

    @Test
    void asyncCreateWithParentPassesParentIdToService() {
        assertThat(asyncCreateWithParentCompilation).succeeded();
        assertThat(asyncCreateWithParentCompilation)
                .generatedSourceFile("rest.asynccreatewithparent.application.TaskService")
                .contentsAsUtf8String()
                .contains("String projectId");
        assertThat(asyncCreateWithParentCompilation)
                .generatedSourceFile("rest.asynccreatewithparent.application.TaskService")
                .contentsAsUtf8String()
                .contains("new Reference<>(projectId)");
    }

    @Test
    void asyncCreateWithNonVoidReturnFailsCompilation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/asynccreatenonvoidreturn/source/Order.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("void return type");
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
        assertThat(asyncUpdateOrderCompilation).succeeded();
        assertThat(asyncUpdateOrderCompilation)
                .generatedSourceFile("rest.asyncupdate.infrastructure.http.OrderController")
                .contentsAsUtf8String()
                .contains("accepted()");
        assertThat(asyncUpdateOrderCompilation)
                .generatedSourceFile("rest.asyncupdate.application.OrderService")
                .contentsAsUtf8String()
                .doesNotContain("orderRepository.save");
    }

    @Test
    void asyncUpdateReturns404WhenAggregateNotFound() {
        assertThat(asyncUpdateOrderCompilation).succeeded();
        assertThat(asyncUpdateOrderCompilation)
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
        assertThat(asyncPathVariableTicketCompilation).succeeded();
        assertThat(asyncPathVariableTicketCompilation)
                .generatedSourceFile("rest.asyncpathvariable.application.OpenRequest")
                .contentsAsUtf8String()
                .doesNotContain("String queue");
        assertThat(asyncPathVariableTicketCompilation)
                .generatedSourceFile("rest.asyncpathvariable.application.OpenRequest")
                .contentsAsUtf8String()
                .contains("String subject");
    }

    @Test
    void asyncCreatePathVariableIsPassedDirectlyToFactory() {
        assertThat(asyncPathVariableTicketCompilation).succeeded();
        assertThat(asyncPathVariableTicketCompilation)
                .generatedSourceFile("rest.asyncpathvariable.application.TicketService")
                .contentsAsUtf8String()
                .contains("String queue");
        assertThat(asyncPathVariableTicketCompilation)
                .generatedSourceFile("rest.asyncpathvariable.application.TicketService")
                .contentsAsUtf8String()
                .contains("Ticket.open(queue, request.subject())");
    }

    @Test
    void asyncUpdatePathVariableIsExcludedFromRequestRecord() {
        assertThat(asyncPathVariableTicketCompilation).succeeded();
        var unexpectedFiles = asyncPathVariableTicketCompilation.generatedSourceFiles().stream()
                .map(javax.tools.JavaFileObject::getName)
                .filter(name -> name.contains("TicketTransitionRequest"))
                .toList();
        assertTrue(unexpectedFiles.isEmpty(),
                () -> "Expected no files with 'TicketTransitionRequest' but found: " + unexpectedFiles);
    }

    @Test
    void asyncCreatePathVariableIsPassedFromControllerToService() {
        assertThat(asyncPathVariableTicketCompilation).succeeded();
        assertThat(asyncPathVariableTicketCompilation)
                .generatedSourceFile("rest.asyncpathvariable.infrastructure.http.TicketController")
                .contentsAsUtf8String()
                .contains("service.open(queue, request)");
    }

    @Test
    void asyncUpdatePathVariableIsPassedFromControllerToService() {
        assertThat(asyncPathVariableTicketCompilation).succeeded();
        assertThat(asyncPathVariableTicketCompilation)
                .generatedSourceFile("rest.asyncpathvariable.infrastructure.http.TicketController")
                .contentsAsUtf8String()
                .contains("service.transition(id, status)");
    }

}
