package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AutocompletePluginTest {

    @Test
    void jdbcRepositoryMethodUsesDistinctSelectForAutocompleteFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/autocomplete/source/Product.java"));

        assertThat(compilation).succeeded();
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
}

