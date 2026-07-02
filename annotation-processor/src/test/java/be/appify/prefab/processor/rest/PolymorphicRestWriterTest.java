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
        var content = assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeController")
                .contentsAsUtf8String();
        
        // Verify controller structure
        content.contains("public class ShapeController");
        content.contains("@RestController");
        content.contains("@RequestMapping");
        content.contains("path = \"shapes\"");
        content.contains("private final ShapeService service");
        
        // Verify create operation
        content.contains("public ResponseEntity<Void> create(@Valid @RequestBody CreateShapeRequest request)");
        content.contains("@Operation");
        content.contains("Create a new Shape");
    }

    @Test
    void polymorphicAggregateResponseTypeContent() {
        assertThat(shapeCompilation).succeeded();
        var content = assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeResponse")
                .contentsAsUtf8String();
        
        // Verify response sealed interface structure
        content.contains("public sealed interface ShapeResponse");
        content.contains("permits ShapeResponse.CircleResponse, ShapeResponse.RectangleResponse");
        content.contains("static ShapeResponse from(Shape aggregate)");
        content.contains("@JsonTypeInfo");
        content.contains("@JsonSubTypes");
        content.contains("record CircleResponse");
        content.contains("record RectangleResponse");
    }

    @Test
    void polymorphicAggregateServiceContent() {
        assertThat(shapeCompilation).succeeded();
        var content = assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.application.ShapeService")
                .contentsAsUtf8String();
        
        // Verify service structure
        content.contains("public class ShapeService");
        content.contains("@Component");
        content.contains("@Transactional");
        content.contains("private final ShapeRepository shapeRepository");
        content.contains("public String createCircle(@Valid CreateShapeRequest.CreateCircleRequest request)");
        content.contains("public String createRectangle(@Valid CreateShapeRequest.CreateRectangleRequest request)");
        content.contains("public Optional<Shape> getById(String id)");
        content.contains("public Page<Shape> getList(Pageable pageable)");
    }

    @Test
    void polymorphicAggregateGeneratesUnionRequestType() {
        assertThat(shapeCompilation).succeeded();
        var content = assertThat(shapeCompilation)
                .generatedSourceFile("rest.polymorphic.application.CreateShapeRequest")
                .contentsAsUtf8String();
        
        // Verify sealed union request interface structure
        content.contains("public sealed interface CreateShapeRequest");
        content.contains("permits CreateShapeRequest.CreateCircleRequest, CreateShapeRequest.CreateRectangleRequest");
        content.contains("@JsonTypeInfo");
        content.contains("record CreateCircleRequest");
        content.contains("record CreateRectangleRequest");
        content.contains("implements CreateShapeRequest");
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
