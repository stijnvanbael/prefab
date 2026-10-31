package be.appify.prefab.processor;

import com.google.testing.compile.JavaFileObjects;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericEventVersioningTest {
    @TempDir Path baselines;

    @Test
    void baselinesAndMigrationCoverageAreIndependentPerBinding() throws Exception {
        var source = """
                package generic;
                import be.appify.prefab.core.annotations.*;
                import be.appify.prefab.core.versioning.Migration;
                @Event(topic="strings", typeArguments=String.class, version="%s")
                @Event(topic="integers", typeArguments=Integer.class, version="%s")
                public record Changed<T>(T payload) {}
                %s
                """;
        var compiler = javac().withOptions("-Aprefab.events.baselineDir=" + baselines);
        assertThat(compiler.withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("generic.Changed", source.formatted("1", "1", "")))).succeeded();
        try (var files = Files.list(baselines)) {
            var paths = files.toList();
            assertEquals(2, paths.size());
            var contracts = paths.stream().map(path -> {
                try { return Files.readString(path); }
                catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            }).toList();
            assertTrue(contracts.stream().anyMatch(text -> text.contains("java.lang.String")));
            assertTrue(contracts.stream().anyMatch(text -> text.contains("java.lang.Integer")));
        }
        var migration = """
                record Old<T>(T payload) {}
                @EventMigration(from="1", to="2")
                class Strings implements Migration<Old<String>,Changed<String>> {
                    public Changed<String> migrate(Old<String> old) { return new Changed<>(old.payload()); }
                }
                """;
        assertThat(compiler.withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("generic.Changed", source.formatted("2", "1", migration)))).succeeded();
        assertThat(compiler.withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("generic.Changed", source.formatted("2", "2", migration))))
                .hadErrorContaining("requires a @EventMigration chain");
    }
}
