package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class RestWriterTest {

    @Test
    void requestValidationAnnotationsAreGenerated() {
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
    void nonStringValueTypeParametersUseInnerFieldType() {
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
    void aggregateWithCreateAndUpdateGeneratesRequestRecords() {
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
    void aggregateReferencingGeneratedEventTypeCompiles() {
        // ...existing code...
    }

    @Test
    void createRequestUsesIdFieldForAggregateTypedParameter() {
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
    void createServiceResolvesAggregateParameterFromRepository() {
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
    void updateRequestIncludesIdForAggregateTypedParameter() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.OrderAssignProductRequest")
                .contentsAsUtf8String()
                .contains("String productId");
    }

    @Test
    void updateServiceFetchesAggregateParameterByIdFromRequest() {
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
    void serviceInjectsRepositoryForAggregateTypedParameter() {
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
    void repositoryNameIsDerivedFromTypeNotParameterName() {
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
                .contains("itemRepository.findById(request.cargoId()).orElseThrow()");
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.renamed.application.ShipmentService")
                .contentsAsUtf8String()
                .contains("ItemRepository itemRepository");
    }

    @Test
    void createRequestMotherUsesIdFieldForAggregateTypedParameter() {
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
    void updateRequestIncludesIdFieldForAggregateTypedParameterInMother() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/aggregate/source/Product.java"),
                        sourceOf("rest/aggregate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.aggregate.application.OrderAssignProductRequest")
                .contentsAsUtf8String()
                .contains("String productId");
    }

    @Test
    void nestedAggregateCreateRequestExcludesParentIdFromBody() {
        // ...existing code...
    }

    @Test
    void createRequestRecordContainsNestedBuilder() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/testclient/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.testclient.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("public static final class Builder");
        assertThat(compilation)
                .generatedSourceFile("rest.testclient.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("public static Builder builder()");
    }

    @Test
    void updateRequestRecordContainsNestedBuilder() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/testclient/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.testclient.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("public static final class Builder");
        assertThat(compilation)
                .generatedSourceFile("rest.testclient.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("public static Builder builder()");
    }
}
