package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class PolymorphicRestWriterTest {

    private static final Compilation shapeCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/polymorphic/source/Shape.java"));
    public static final Compilation polymorphicWithParentCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("rest/polymorphicwithparent/source/Canvas.java"),
                    sourceOf("rest/polymorphicwithparent/source/Drawing.java"));

    @Test
    void polymorphicAggregateGeneratesController() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeController")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateGeneratesResponseType() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeResponse")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateGeneratesService() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.application.ShapeService")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateControllerContent() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeController")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/ShapeController.java"));
    }

    @Test
    void polymorphicAggregateResponseTypeContent() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeResponse")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/ShapeResponse.java"));
    }

    @Test
    void polymorphicAggregateServiceContent() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.application.ShapeService")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/ShapeService.java"));
    }

    @Test
    void polymorphicAggregateGeneratesUnionRequestType() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.application.CreateShapeRequest")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/CreateShapeRequest.java"));
    }

    @Test
    void polymorphicAggregateControllerHasDispatchMethod() {
        assertThat(shapeCompilation).succeeded();
        assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeController")
                .contentsAsUtf8String()
                .contains("ResponseEntity<Void> create(");
    }

    @Test
    void polymorphicAggregateWithParentGeneratesControllerWithParentPathVariable() {
        assertThat(polymorphicWithParentCompilation).succeeded();
        assertThat(polymorphicWithParentCompilation)
                .generatedSourceFile("rest.polymorphicwithparent.infrastructure.http.DrawingController")
                .contentsAsUtf8String()
                .contains("canvas/{canvasId}/drawings");
    }

    @Test
    void polymorphicAggregateWithInterfaceParentAnnotationGeneratesCorrectPath() {
        assertThat(polymorphicWithParentCompilation).succeeded();
        assertThat(polymorphicWithParentCompilation)
                .generatedSourceFile("rest.polymorphicwithparent.application.DrawingService")
                .contentsAsUtf8String()
                .contains("DrawingRepository");
    }

    @Test
    void polymorphicAggregateWithParentGeneratesTestClientWithParentParameter() {
        assertThat(polymorphicWithParentCompilation).succeeded();
        assertThat(polymorphicWithParentCompilation)
                .generatedSourceFile("rest.polymorphicwithparent.infrastructure.http.DrawingController")
                .contentsAsUtf8String()
                .contains("String canvasId");
    }

    @Test
    void polymorphicAggregateWithParentUpdateRequestDoesNotContainParentField() {
        assertThat(polymorphicWithParentCompilation).succeeded();
        assertThat(polymorphicWithParentCompilation)
                .generatedSourceFile("rest.polymorphicwithparent.application.CircleResizeRequest")
                .contentsAsUtf8String()
                .doesNotContain("canvas");
    }
}
