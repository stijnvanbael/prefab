package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ComputedFieldTest {

    private static final Compilation computedOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/computed/source/Order.java"));

    private static final Compilation computedShapeCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/computed/polymorphic/source/Shape.java"));

    @Test
    void responseRecordContainsComputedFields() {
        assertThat(computedOrderCompilation).succeeded();
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.infrastructure.http.OrderResponse")
                .contentsAsUtf8String()
                .contains("BigDecimal total");
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.infrastructure.http.OrderResponse")
                .contentsAsUtf8String()
                .contains("int lineCount");
    }

    @Test
    void responseFromMethodPopulatesComputedFields() {
        assertThat(computedOrderCompilation).succeeded();
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.infrastructure.http.OrderResponse")
                .contentsAsUtf8String()
                .contains("aggregateRoot.total()");
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.infrastructure.http.OrderResponse")
                .contentsAsUtf8String()
                .contains("aggregateRoot.lineCount()");
    }

    @Test
    void createRequestDoesNotContainComputedFields() {
        assertThat(computedOrderCompilation).succeeded();
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.application.CreateOrderRequest")
                .contentsAsUtf8String()
                .doesNotContain("total");
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.application.CreateOrderRequest")
                .contentsAsUtf8String()
                .doesNotContain("lineCount");
    }

    @Test
    void updateRequestDoesNotContainComputedFields() {
        assertThat(computedOrderCompilation).succeeded();
        assertThat(computedOrderCompilation)
                .generatedSourceFile("rest.computed.application.OrderAddLineRequest")
                .contentsAsUtf8String()
                .doesNotContain("total");
    }

    @Test
    void polymorphicSubtypeResponseContainsComputedField() {
        assertThat(computedShapeCompilation).succeeded();
        assertThat(computedShapeCompilation)
                .generatedSourceFile("rest.computed.polymorphic.infrastructure.http.ShapeResponse")
                .contentsAsUtf8String()
                .contains("double area");
        assertThat(computedShapeCompilation)
                .generatedSourceFile("rest.computed.polymorphic.infrastructure.http.ShapeResponse")
                .contentsAsUtf8String()
                .contains("subtype.area()");
    }

    @Test
    void computedMethodWithArgumentsFailsCompilation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/computed/invalidargs/source/Order.java"));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining("@Computed method discounted must be public, take no arguments and return a value");
    }

    @Test
    void computedMethodClashingWithFieldFailsCompilation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/computed/nameclash/source/Order.java"));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining("@Computed method amount clashes with a field of the same name");
    }
}
