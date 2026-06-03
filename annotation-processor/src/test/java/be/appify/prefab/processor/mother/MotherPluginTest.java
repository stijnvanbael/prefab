package be.appify.prefab.processor.mother;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.tools.StandardLocation;

import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotherPluginTest {
    private static final Compilation personCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("mother/person/source/Person.java"));
    private static final Compilation personEventCompilation =  javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("mother/events/source/PersonEvent.java"));
    private static final Compilation orderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("mother/nullablerecord/source/ShipmentEvent.java"),
                    sourceOf("mother/nullablerecord/source/Order.java"));

    @Test
    void requestRecordContainsSchemaExampleAnnotationFromExampleOnConstructorParam() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("example = \"Alice\"");
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("example = \"alice@example.com\"");
    }

    @Test
    void updateRequestRecordContainsSchemaExampleAnnotation() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("example = \"Bob\"");
    }

    @Test
    void responseRecordContainsSchemaExampleAnnotationFromAggregateField() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("example = \"Alice\"");
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("example = \"alice@example.com\"");
    }

    @Test
    void requestRecordContainsSchemaDescriptionAnnotationFromDocOnConstructorParam() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("description = \"Full name of the person\"");
    }

    @Test
    void updateRequestRecordContainsSchemaDescriptionAnnotation() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("description = \"Full name of the person\"");
    }

    @Test
    void responseRecordContainsSchemaDescriptionAnnotationFromAggregateField() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
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
    void eventMotherBuilderGeneratedAsProductionSourceFile() {
        assertThat(personEventCompilation).succeeded();
        assertThat(personEventCompilation)
                .generatedSourceFile("mother.events.source.PersonEventCreatedBuilder")
                .contentsAsUtf8String()
                .contains("public class PersonEventCreatedBuilder");
    }

    @Test
    void eventMotherDelegatesDefaultsToStandaloneBuilder() {
        assertThat(personEventCompilation).succeeded();
        assertThat(personEventCompilation)
                .generatedSourceFile("mother.events.source.PersonEventCreatedBuilder")
                .contentsAsUtf8String()
                .doesNotContain("class Builder");
    }
    @Test
    void handWrittenEventMotherGeneratesStandaloneBuilderClass() {
        assertThat(personEventCompilation).succeeded();
        assertThat(personEventCompilation)
                .generatedSourceFile("mother.events.source.PersonEventCreatedBuilder")
                .contentsAsUtf8String()
                .contains("public class PersonEventCreatedBuilder");
    }

    @Test
    void motherUsesExampleValueFromInnerFieldOfSingleValueType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/singlewithexample/source/Invoice.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/singlewithexample/source", "CreateInvoiceRequestMother.java")
                .contentsAsUtf8String()
                .contains("\"INV-001\"");
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
    void compilationSucceedsForAggregateWithFieldTypeContainingStaticConstants() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("mother/staticconstants/source/AgentRole.java"),
                        sourceOf("mother/staticconstants/source/AgentTask.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void builderForRecordWithStaticConstantsContainsOnlyInstanceComponents() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("mother/staticconstants/source/AgentRole.java"),
                        sourceOf("mother/staticconstants/source/AgentTask.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.staticconstants.source.application.CreateAgentTaskRequest")
                .contentsAsUtf8String()
                .contains("String assignedRole");
        assertThat(compilation)
                .generatedSourceFile("mother.staticconstants.source.application.CreateAgentTaskRequest")
                .contentsAsUtf8String()
                .doesNotContain("PLANNER");
        assertThat(compilation)
                .generatedSourceFile("mother.staticconstants.source.application.CreateAgentTaskRequest")
                .contentsAsUtf8String()
                .doesNotContain("RESEARCHER");
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
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("public static CreatePersonRequest.Builder<?> builder()");
        assertThat(personCompilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("public static class Builder");
    }

    @Test
    void requestMotherHasConsumerOverload() {
        assertThat(personCompilation).succeeded();
        assertThat(personCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/person", "CreatePersonRequestMother.java")
                .contentsAsUtf8String()
                .contains("Consumer");
    }

    @Test
    void eventMotherHasConsumerOverload() {
        assertThat(personEventCompilation).succeeded();
        assertThat(personEventCompilation)
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

    @Test
    void eventMotherWithMainOutputTargetIsWrittenToMainSource() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/mainoutput/source/OrderShipped.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.mainoutput.source.OrderShippedMother")
                .contentsAsUtf8String()
                .contains("class OrderShippedMother");
        assertTrue(compilation.generatedFiles().stream()
                .noneMatch(file -> file.getName().contains("CLASS_OUTPUT")
                        && file.getName().endsWith("/mother/mainoutput/source/OrderShippedMother.java")),
                "Event mother must not be written to test (CLASS_OUTPUT) when target = MAIN");
    }

    @Test
    void eventMotherWithNullableRecordFieldGeneratesWithoutOverload() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "ShipmentEventMother.java")
                .contentsAsUtf8String()
                .contains("withoutAddress");
    }

    @Test
    void eventMotherWithListOfRecordFieldGeneratesVarargsOverload() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "ShipmentEventMother.java")
                .contentsAsUtf8String()
                .contains("itemsCustomisers");
    }

    @Test
    void eventMotherWithListOfRecordFieldGeneratesEmptyOverload() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "ShipmentEventMother.java")
                .contentsAsUtf8String()
                .contains("emptyItems");
    }

    @Test
    void eventMotherWithNullableListOfRecordFieldGeneratesWithoutOverload() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "ShipmentEventMother.java")
                .contentsAsUtf8String()
                .contains("withoutOptionalItems");
    }

    @Test
    void nonNullableRecordFieldDoesNotGenerateWithoutOverload() {
        assertThat(orderCompilation).succeeded();
        // items is non-nullable, so only withoutOptionalItems should appear, not withoutItems
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "ShipmentEventMother.java")
                .contentsAsUtf8String()
                .doesNotContain("withoutItems(");
    }

    @Test
    void requestMotherWithNullableRecordFieldGeneratesWithoutOverload() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "CreateOrderRequestMother.java")
                .contentsAsUtf8String()
                .contains("withoutShippingAddress");
    }

    @Test
    void requestMotherWithListOfRecordFieldGeneratesVarargsAndEmptyOverloads() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "CreateOrderRequestMother.java")
                .contentsAsUtf8String()
                .contains("itemsCustomisers");
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mother/nullablerecord/source", "CreateOrderRequestMother.java")
                .contentsAsUtf8String()
                .contains("emptyItems");
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
