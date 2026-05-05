package be.appify.prefab.processor.mother;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotherPluginTest {

    @Test
    void compilationSucceedsWithExampleAnnotations() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void requestRecordContainsSchemaExampleAnnotationFromExampleOnConstructorParam() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("example = \"Alice\"");
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("example = \"alice@example.com\"");
    }

    @Test
    void updateRequestRecordContainsSchemaExampleAnnotation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("example = \"Bob\"");
    }

    @Test
    void responseRecordContainsSchemaExampleAnnotationFromAggregateField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("example = \"Alice\"");
        assertThat(compilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("example = \"alice@example.com\"");
    }

    @Test
    void requestRecordContainsSchemaDescriptionAnnotationFromDocOnConstructorParam() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("description = \"Full name of the person\"");
    }

    @Test
    void updateRequestRecordContainsSchemaDescriptionAnnotation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("description = \"Full name of the person\"");
    }

    @Test
    void responseRecordContainsSchemaDescriptionAnnotationFromAggregateField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("description = \"Full name of the person\"");
    }

    @Test
    void invalidExampleOnNumericFieldProducesCompilerError() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/invalideexample/source/BadExample.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot be parsed as int");
    }

    @Test
    void compilationSucceedsForEventTypes() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/events/source/PersonEvent.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void eventMotherBuilderGeneratedAsProductionSourceFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/events/source/PersonEvent.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.events.source.PersonEventCreatedBuilder")
                .contentsAsUtf8String()
                .contains("public class PersonEventCreatedBuilder");
    }

    @Test
    void eventMotherDelegatesDefaultsToStandaloneBuilder() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/events/source/PersonEvent.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.events.source.PersonEventCreatedBuilder")
                .contentsAsUtf8String()
                .doesNotContain("class Builder");
    }

    @Test
    void motherGeneratedForMultiFieldRecordNestedInsideSingleValueType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/nestedinwrapper/source/OrderEvent.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void motherUsesMockMultipartFileForBinaryField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/binary/source/Document.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void noMotherGeneratedForUpdateWithOnlyParentParameter() {
        var organisationSource = sourceOf("mother/childwithparent/source/Organisation.java");
        var memberSource = sourceOf("mother/childwithparent/source/Member.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(organisationSource, memberSource);

        assertThat(compilation).succeeded();
        var generatedNames = compilation.generatedSourceFiles().stream()
                .map(javax.tools.JavaFileObject::getName)
                .toList();
        assertTrue(generatedNames.stream().noneMatch(name -> name.contains("MemberMoveRequest")));
    }

    @Test
    void motherUsesEmptyMapForMapField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/withmap/source/InventoryEvent.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/withmap/source", "InventoryEventUpdatedMother.java")
                .contentsAsUtf8String()
                .contains("Map.of()");
    }

    @Test
    void motherUsesRecordBuilderInsteadOfCanonicalConstructor() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("public static Builder builder()");
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("public static final class Builder");
    }

    @Test
    void requestMotherHasConsumerOverload() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/person", "CreatePersonRequestMother.java")
                .contentsAsUtf8String()
                .contains("Consumer");
    }

    @Test
    void eventMotherHasConsumerOverload() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/events/source/PersonEvent.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/events/source", "PersonEventCreatedMother.java")
                .contentsAsUtf8String()
                .contains("Consumer");
    }

    @Test
    void dependencyEventDoesNotGenerateMotherBuilder() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("mother/dependency/source/DependencyEvent.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("mother/dependencyconsumer/source/DependencyConsumer.java"));

            assertThat(compilation).succeeded();
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/mother/dependency/source/DependencyEventBuilder.java")));
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/mother/dependency/source/DependencyEventMother.java")));
        } finally {
            deleteRecursively(dependencyClasspath);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
