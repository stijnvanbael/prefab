package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AvscPluginTest {

    @Test
    void simpleAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("@Event(");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("topic = \"simple-avsc\"");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("serialization = Event.Serialization.AVRO");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("@Namespace(\"event.avsc\")");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("String name");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("int age");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("double score");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("boolean active");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("implements SimpleAvsc");
    }

    @Test
    void nonPrimitiveAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nonprimitive/source/NonPrimitiveAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.NonPrimitiveAvscEvent")
                .contentsAsUtf8String()
                .contains("Instant timestamp");
        assertThat(compilation).generatedSourceFile("event.avsc.NonPrimitiveAvscEvent")
                .contentsAsUtf8String()
                .contains("LocalDate date");
        assertThat(compilation).generatedSourceFile("event.avsc.NonPrimitiveAvscEvent")
                .contentsAsUtf8String()
                .contains("Duration duration");
    }

    @Test
    void nullableAvscEventSchemaFactoryGeneratesNullableUnion() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullable/source/NullableAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("SchemaSupport.createNullableSchema(Schema.create(Schema.Type.STRING))");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("verifySchemaCompatibility(this.schema)");
    }

    @Test
    void nullableAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullable/source/NullableAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("String name");
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("@Nullable");
        assertThat(compilation).generatedSourceFile("event.avsc.NullableAvscEvent")
                .contentsAsUtf8String()
                .contains("String description");
    }

    @Test
    void arrayAvscEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/array/source/ArrayAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("List<String> tags");
        assertThat(compilation).generatedSourceFile("event.avsc.ArrayAvscEvent")
                .contentsAsUtf8String()
                .contains("implements ArrayAvsc");
    }

    @Test
    void interfaceNameCollidingWithRecordNameReportsError() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/array/source/ArrayAvscEvent.java"));
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("conflicts with the contract interface name");
    }

    @Test
    void simpleAvscEventGeneratesSchemaFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("verifySchemaCompatibility(this.schema)");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("Schema.Parser().parse(stream)");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscEventToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.GenericRecordToSimpleAvscEventConverter")
                .isNotNull();
    }

    @Test
    void multiPathAvscSchemaFactoriesLoadFromCorrectAvscFiles() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/multi/source/MultiAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.multi.infrastructure.avro.MultiAvscEventASchemaFactory")
                .contentsAsUtf8String()
                .contains("verifySchemaCompatibility(this.schema)");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.multi.infrastructure.avro.MultiAvscEventBSchemaFactory")
                .contentsAsUtf8String()
                .contains("verifySchemaCompatibility(this.schema)");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.multi.infrastructure.avro.MultiAvscEventASchemaFactory")
                .contentsAsUtf8String()
                .contains("getResourceAsStream(\"event/avsc/multi/source/MultiAvscEventA.avsc\")");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.multi.infrastructure.avro.MultiAvscEventBSchemaFactory")
                .contentsAsUtf8String()
                .contains("getResourceAsStream(\"event/avsc/multi/source/MultiAvscEventB.avsc\")");
    }

    @Test
    void multiPathAvscGeneratesOneRecordPerSchema() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/multi/source/MultiAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventA")
                .contentsAsUtf8String()
                .contains("String id");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventA")
                .contentsAsUtf8String()
                .contains("double amount");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventA")
                .contentsAsUtf8String()
                .contains("implements MultiAvsc");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventB")
                .contentsAsUtf8String()
                .contains("String reference");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventB")
                .contentsAsUtf8String()
                .contains("int count");
        assertThat(compilation).generatedSourceFile("event.avsc.multi.MultiAvscEventB")
                .contentsAsUtf8String()
                .contains("implements MultiAvsc");
    }

    @Test
    void sealedInterfaceWithPermitsReferencingGeneratedTypesCompiles() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/sealed/source/SealedMultiAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.sealed.SealedMultiAvscEventA")
                .contentsAsUtf8String()
                .contains("implements SealedMultiAvsc");
        assertThat(compilation).generatedSourceFile("event.avsc.sealed.SealedMultiAvscEventB")
                .contentsAsUtf8String()
                .contains("implements SealedMultiAvsc");
    }

    @Test
    void sealedInterfaceCompilesWhenAvscNamespaceDiffersFromContractPackage() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/sealedmismatch/source/SealedMismatchAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.sealedmismatch.MeteringconfigUpdated")
                .contentsAsUtf8String()
                .contains("implements SealedMismatchAvsc");
        assertThat(compilation).generatedSourceFile("event.avsc.sealedmismatch.MeteringconfigUpdated")
                .contentsAsUtf8String()
                .contains("@Namespace(\"intern.dcs.meteringconfig.facts.v1\")");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.sealedmismatch.infrastructure.avro.MeteringconfigUpdatedSchemaFactory")
                .contentsAsUtf8String()
                .contains("intern.dcs.meteringconfig.facts.v1");
    }

    @Test
    void generatedAvscEventRecordContainsNestedBuilder() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("public static final class Builder");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("public static Builder builder()");
    }

    @Test
    void nullableNestedRecordWithNullableEnumGeneratesUnionSafeConverters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullablenestedenum/source/NullableNestedEnumAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableNestedEnumAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("verifySchemaCompatibility(this.schema)");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableNestedEnumAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("intern.dcs.meteringconfig.facts.v1");

        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.StatusSchemaFactory")
                .contentsAsUtf8String()
                .doesNotContain("verifySchemaCompatibility(this.schema)");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.Status")
                .contentsAsUtf8String()
                .contains("@Namespace(\"intern.dcs.meteringconfig.facts.v1\")");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.StatusSchemaFactory")
                .contentsAsUtf8String()
                .contains("intern.dcs.meteringconfig.facts.v1");

        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableNestedEnumAvscEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .contains("genericRecord.put(\"status\", event.status() != null ? statusToGenericRecordConverter.convert(event.status()) : null);");

        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.StatusToGenericRecordConverter")
                .contentsAsUtf8String()
                .contains("SchemaSupport.enumSchemaOf(schema.getField(\"statusInactiefReden\").schema())");

        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.StatusToGenericRecordConverter")
                .contentsAsUtf8String()
                .contains("genericRecord.put(\"statusInactiefReden\", event.statusInactiefReden() != null ?");
    }

    @Test
    void nullableSingleValuedNestedRecordUsesRecordBranchInSchemaFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullablesinglevaluedrecord/source/NullableSingleValuedRecordAvsc.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.NullableSingleValuedRecordAvscEventSchemaFactory")
                .contentsAsUtf8String()
                .contains("new Schema.Field(\"eanGsrn\", Schema.create(Schema.Type.STRING))");
    }

    @Test
    void avscArtifactsFromDependencyAreNotRegeneratedInConsumerModule() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("event/avsc/dependency/source/DependencyAvsc.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("event/avsc/dependencyconsumer/source/DependencyConsumer.java"));

            assertThat(compilation).succeeded();
            assertFalse(compilation.generatedSourceFiles().stream()
                    .anyMatch(file -> file.toUri().getPath().endsWith("/event/avsc/dependency/DependencyAvscEvent.java")));
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/avsc/dependency/infrastructure/avro/DependencyAvscEventSchemaFactory.java")));
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/avsc/dependency/infrastructure/avro/DependencyAvscEventToGenericRecordConverter.java")));
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/avsc/dependency/infrastructure/avro/GenericRecordToDependencyAvscEventConverter.java")));
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
