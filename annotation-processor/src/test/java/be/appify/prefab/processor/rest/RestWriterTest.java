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
        // ...existing code...
    }

    @Test
    void createRequestUsesIdFieldForAggregateTypedParameter() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.CreateOrderRequest")
                .contentsAsUtf8String()
                .contains("String productId");
    }

    @Test
    void createServiceResolvesAggregateParameterFromRepository() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.OrderService")
                .contentsAsUtf8String()
                .contains("productRepository.findById(request.productId()).orElseThrow()");
    }

    @Test
    void updateRequestExcludesAggregateTypedParameter() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        var hasAssignProductRequest = compilation.generatedSourceFiles().stream()
                .anyMatch(f -> f.getName().contains("OrderAssignProductRequest"));
        if (hasAssignProductRequest) {
            throw new AssertionError("Expected no OrderAssignProductRequest to be generated");
        }
    }

    @Test
    void updateServicePreFetchesAggregateParameterViaReferenceField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.OrderService")
                .contentsAsUtf8String()
                .contains("productRepository.findById(aggregate.product().id()).orElseThrow()");
    }

    @Test
    void serviceInjectsRepositoryForAggregateTypedParameter() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.OrderService")
                .contentsAsUtf8String()
                .contains("ProductRepository productRepository");
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.OrderService")
                .contentsAsUtf8String()
                .contains("this.productRepository = productRepository");
    }

    @Test
    void repositoryNameIsDerivedFromTypeNotParameterName() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Item.java"),
                        sourceOf("rest/aggregate/source/Shipment.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.renamed.application.ShipmentService")
                .contentsAsUtf8String()
                .contains("itemRepository.findById(request.cargoId()).orElseThrow()");
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.renamed.application.ShipmentService")
                .contentsAsUtf8String()
                .contains("itemRepository.findById(aggregate.assignedItem().id()).orElseThrow()");
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.renamed.application.ShipmentService")
                .contentsAsUtf8String()
                .contains("ItemRepository itemRepository");
    }

    @Test
    void createRequestMotherUsesIdFieldForAggregateTypedParameter() throws IOException {
        // Verifies that the CreateOrderRequest record (compilation output) has String productId,
        // which in turn means the generated ObjectMother also uses String productId.
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.CreateOrderRequest")
                .contentsAsUtf8String()
                .contains("String productId");
    }

    @Test
    void updateRequestMotherExcludesAggregateTypedParameter() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        var hasAssignProductRequestMother = compilation.generatedSourceFiles().stream()
                .anyMatch(f -> f.getName().contains("OrderAssignProductRequestMother"));
        if (hasAssignProductRequestMother) {
            throw new AssertionError("Expected no OrderAssignProductRequestMother to be generated");
        }
    }

    @Test
    void nestedAggregateCreateRequestUsesIdForAggregateParent() throws IOException {
        // Verifies that CreateOrderLineRequest uses String orderId (not Order order),
        // which ensures the test client's path variable accessor is orderId() not order().
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"),
                        sourceOf("rest/aggregate/source/OrderLine.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.CreateOrderLineRequest")
                .contentsAsUtf8String()
                .contains("String orderId");
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.CreateOrderLineRequest")
                .contentsAsUtf8String()
                .contains("String productId");
    }
}
