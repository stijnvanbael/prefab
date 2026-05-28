package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabProcessor;
import be.appify.prefab.processor.TestClientWriter;
import be.appify.prefab.processor.TestFileOutput;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AutocompletePluginTest {

    @Test
    void autocompleteGeneratesControllerServiceAndRepositoryMethods() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/autocomplete/source/Product.java"));

        assertThat(compilation).succeeded();

        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.infrastructure.http.ProductController")
                .contentsAsUtf8String()
                .contains("ResponseEntity<List<String>> autocompleteByName(@RequestParam String query)");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.infrastructure.http.ProductController")
                .contentsAsUtf8String()
                .contains("RequestMethod.GET");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.infrastructure.http.ProductController")
                .contentsAsUtf8String()
                .contains("path = \"/name/autocomplete\"");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.infrastructure.http.ProductController")
                .contentsAsUtf8String()
                .contains("path = \"/brands/search\"");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.infrastructure.http.ProductController")
                .contentsAsUtf8String()
                .contains("return ResponseEntity.ok(service.autocompleteByName(query));");

        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductService")
                .contentsAsUtf8String()
                .contains("List<String> autocompleteByName(String query)");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductService")
                .contentsAsUtf8String()
                .contains("productRepository.autocompleteByName(query, PageRequest.of(0, 10))");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductService")
                .contentsAsUtf8String()
                .contains("productRepository.autocompleteByBrand(query, PageRequest.of(0, 10))");

        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductRepository")
                .contentsAsUtf8String()
                .contains("@Query(\"SELECT DISTINCT \\\"name\\\" FROM \\\"product\\\" WHERE LOWER(\\\"name\\\") LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY \\\"name\\\"\")");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductRepository")
                .contentsAsUtf8String()
                .contains("@Query(\"SELECT DISTINCT \\\"brand\\\" FROM \\\"product\\\" WHERE \\\"brand\\\" LIKE CONCAT('%', :query, '%') ORDER BY \\\"brand\\\"\")");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductRepository")
                .contentsAsUtf8String()
                .contains("List<String> autocompleteByName(@Param(\"query\") String query, Pageable pageable);");
        assertThat(compilation)
                .generatedSourceFile("rest.autocomplete.application.ProductRepository")
                .contentsAsUtf8String()
                .contains("List<String> autocompleteByBrand(@Param(\"query\") String query, Pageable pageable);");
    }

    @Test
    void autocompleteGeneratesTestClientMethods() {
        var processor = new CapturingAutocompleteProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(sourceOf("rest/autocomplete/source/Product.java"));

        assertThat(compilation).succeeded();
        var clientSource = processor.capturedSources.stream()
                .filter(source -> source.contains("class ProductClient"))
                .findFirst()
                .orElse("");

        Assertions.assertTrue(clientSource.contains("List<String> autocompleteByName(String query)"));
        Assertions.assertTrue(clientSource.contains("List<String> autocompleteByBrand(String query)"));
        Assertions.assertTrue(clientSource.contains("/products/name/autocomplete"));
        Assertions.assertTrue(clientSource.contains("/products/brands/search"));
        Assertions.assertTrue(clientSource.contains("request.queryParam(\"query\", query);"));
    }

    @SupportedAnnotationTypes({"be.appify.prefab.core.annotations.*"})
    static class CapturingAutocompleteProcessor extends PrefabProcessor {
        final List<String> capturedSources = new ArrayList<>();

        @Override
        protected TestClientWriter createTestClientWriter(PrefabContext context) {
            return new TestClientWriter(context, new CapturingTestFileOutput(capturedSources));
        }
    }

    static class CapturingTestFileOutput implements TestFileOutput {
        private final List<String> capturedSources;

        CapturingTestFileOutput(List<String> capturedSources) {
            this.capturedSources = capturedSources;
        }

        @Override
        public void setPreferredElement(TypeElement element) {
        }

        @Override
        public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
            capturedSources.add(JavaFile.builder(packagePrefix, type).build().toString());
        }
    }
}

