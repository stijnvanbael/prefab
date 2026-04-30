package be.appify.prefab.processor.mother;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
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
                .generatedSourceFile("mother.withmap.source.InventoryEventUpdatedBuilder")
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
}
