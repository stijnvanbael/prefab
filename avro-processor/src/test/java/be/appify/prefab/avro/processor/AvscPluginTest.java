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
import javax.tools.StandardLocation;

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
                .contains("namespace = \"event.avsc\"");
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
                .generatedSourceFile("event.avsc.infrastructure.avro.SimpleAvscToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.GenericRecordToSimpleAvscEventConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.GenericRecordToSimpleAvscConverter")
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
        assertThat(compilation)
                .generatedSourceFile("event.avsc.sealed.infrastructure.avro.SealedMultiAvscToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.sealed.infrastructure.avro.GenericRecordToSealedMultiAvscConverter")
                .isNotNull();
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
                .contains("namespace = \"intern.dcs.meteringconfig.facts.v1\"");
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
                .contains("public static class Builder");
        assertThat(compilation).generatedSourceFile("event.avsc.SimpleAvscEvent")
                .contentsAsUtf8String()
                .contains("public static SimpleAvscEvent.Builder<?> builder()");
    }
    @Test
    void avscEventMotherDelegatesToNestedBuilderNotStandaloneClass() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/simple/source/SimpleAvsc.java"));
        assertThat(compilation).succeeded();
        // The mother must use the nested Builder from the generated record — no standalone class.
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "event/avsc", "SimpleAvscEventMother.java")
                .contentsAsUtf8String()
                .contains("SimpleAvscEvent.Builder");
        assertFalse(
                compilation.generatedFiles().stream()
                        .anyMatch(f -> f.toUri().getPath().endsWith("/event/avsc/SimpleAvscEventBuilder.java")),
                "Standalone SimpleAvscEventBuilder must not be generated when an embedded Builder exists"
        );
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
                .contains("namespace = \"intern.dcs.meteringconfig.facts.v1\"");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.FysiekeStatus")
                .contentsAsUtf8String()
                .contains("namespace = \"intern.dcs.meteringconfig.facts.v1\"");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.StatusInactiefReden")
                .contentsAsUtf8String()
                .contains("namespace = \"intern.dcs.meteringconfig.facts.v1\"");
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
    void nullableSingleValuedNestedRecordInArrayItemUsesRecordBranchInSchemaFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf(
                        "event/avsc/nullablearraynestedsinglevaluedrecord/source/NullableArrayNestedSingleValuedRecordAvsc.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation)
                .generatedSourceFile("event.avsc.infrastructure.avro.ToegangspuntSchemaFactory")
                .contentsAsUtf8String()
                .contains("new Schema.Field(\"gelinktMarkttoegangspunt\", SchemaSupport.createNullableSchema(Schema.createRecord(\"GelinktMarkttoegangspunt\", null, \"intern.dcs.meteringconfig.facts.v1\", false, List.of(");
    }

    @Test
    void docFieldAnnotationIsPropagatedFromAvscToGeneratedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/docfield/source/DocFieldAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.DocFieldAvscEvent")
                .contentsAsUtf8String()
                .contains("@Doc(\"Full name of the person\")");
        assertThat(compilation).generatedSourceFile("event.avsc.DocFieldAvscEvent")
                .contentsAsUtf8String()
                .contains("@Doc(\"An event describing a person\")");
        assertThat(compilation).generatedSourceFile("event.avsc.DocFieldAvscEvent")
                .contentsAsUtf8String()
                .contains("String name");
        assertThat(compilation).generatedSourceFile("event.avsc.DocFieldAvscStatus")
                .contentsAsUtf8String()
                .contains("@Doc(\"The status of the person\")");
    }


    @Test
    void scalarUnionFieldGeneratesNestedSealedInterface() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/scalarunion/source/ScalarUnionAvsc.java"));
        assertThat(compilation).succeeded();
        // Top-level event record with List of nested records
        assertThat(compilation).generatedSourceFile("event.avsc.scalarunion.ScalarUnionAvscEvent")
                .contentsAsUtf8String()
                .contains("List<ScalarUnionItem> items");
        // Nested record with the sealed-interface union field
        assertThat(compilation).generatedSourceFile("event.avsc.scalarunion.ScalarUnionItem")
                .contentsAsUtf8String()
                .contains("ExactValue exactValue");
        // Sealed interface generated from the ["double","string"] union
        assertThat(compilation).generatedSourceFile("event.avsc.scalarunion.ExactValue")
                .contentsAsUtf8String()
                .contains("sealed interface ExactValue");
        // Double branch wrapper
        assertThat(compilation).generatedSourceFile("event.avsc.scalarunion.ExactValueDouble")
                .contentsAsUtf8String()
                .contains("double value");
        // String branch wrapper
        assertThat(compilation).generatedSourceFile("event.avsc.scalarunion.ExactValueString")
                .contentsAsUtf8String()
                .contains("String value");
        // Schema factory for the union emits a union schema
        assertThat(compilation)
                .generatedSourceFile("event.avsc.scalarunion.infrastructure.avro.ExactValueSchemaFactory")
                .contentsAsUtf8String()
                .contains("Schema.createUnion");
        assertFalse(compilation.generatedSourceFiles().stream()
                .anyMatch(file -> file.toUri().getPath().endsWith("/event/avsc/scalarunion/ExactValueMother.java")));
    }

    @Test
    void scalarUnionFieldWithSampleMatchesPermittedStringBranchInMotherDefaults() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/scalarunionsample/source/ScalarUnionSampleAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "event/avsc/scalarunionsample", "ScalarUnionSampleAvscEventMother.java")
                .contentsAsUtf8String()
                .contains("builder.exactValue(new ExactValueString(\"known-value\"))");
    }

        @Test
    void recordUnionFieldGeneratesSealedInterfaceWithRecordBranches() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/recordunion/source/RecordUnionAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.recordunion.RecordUnionAvscEvent")
                .contentsAsUtf8String().contains("Payload payload");
        assertThat(compilation).generatedSourceFile("event.avsc.recordunion.Payload")
                .contentsAsUtf8String().contains("sealed interface Payload");
        assertThat(compilation).generatedSourceFile("event.avsc.recordunion.PayloadTextPayload")
                .contentsAsUtf8String().contains("TextPayload value");
        assertThat(compilation).generatedSourceFile("event.avsc.recordunion.PayloadNumericPayload")
                .contentsAsUtf8String().contains("NumericPayload value");
        assertThat(compilation).generatedSourceFile("event.avsc.recordunion.TextPayload")
                .contentsAsUtf8String().contains("String content");
        assertThat(compilation).generatedSourceFile("event.avsc.recordunion.NumericPayload")
                .contentsAsUtf8String().contains("double value");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.recordunion.infrastructure.avro.PayloadSchemaFactory")
                .contentsAsUtf8String().contains("Schema.createUnion");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.recordunion.infrastructure.avro.TextPayloadToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.recordunion.infrastructure.avro.NumericPayloadToGenericRecordConverter")
                .isNotNull();
        assertFalse(compilation.generatedSourceFiles().stream()
                .anyMatch(file -> file.toUri().getPath().endsWith("/event/avsc/recordunion/PayloadMother.java")));
    }
    @Test
    void enumUnionFieldGeneratesSealedInterfaceWithEnumAndStringBranches() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/enumunion/source/EnumUnionAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.enumunion.EnumUnionAvscEvent")
                .contentsAsUtf8String().contains("Status status");
        assertThat(compilation).generatedSourceFile("event.avsc.enumunion.Status")
                .contentsAsUtf8String().contains("sealed interface Status");
        assertThat(compilation).generatedSourceFile("event.avsc.enumunion.StatusAlertLevel")
                .contentsAsUtf8String().contains("AlertLevel value");
        assertThat(compilation).generatedSourceFile("event.avsc.enumunion.StatusString")
                .contentsAsUtf8String().contains("String value");
        assertThat(compilation).generatedSourceFile("event.avsc.enumunion.AlertLevel")
                .contentsAsUtf8String().contains("LOW");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.enumunion.infrastructure.avro.StatusSchemaFactory")
                .contentsAsUtf8String().contains("Schema.createUnion");
    }
    @Test
    void nullableMultiBranchUnionGeneratesNullableAnnotationAndSealedInterface() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/nullablemultibranch/source/NullableMultiBranchAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.NullableMultiBranchAvscEvent")
                .contentsAsUtf8String().contains("@Nullable");
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.NullableMultiBranchAvscEvent")
                .contentsAsUtf8String().contains("Result result");
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.Result")
                .contentsAsUtf8String().contains("sealed interface Result");
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.ResultSuccessResult")
                .contentsAsUtf8String().contains("SuccessResult value");
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.ResultFailureResult")
                .contentsAsUtf8String().contains("FailureResult value");
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.SuccessResult")
                .contentsAsUtf8String().contains("String message");
        assertThat(compilation).generatedSourceFile("event.avsc.nullablemultibranch.FailureResult")
                .contentsAsUtf8String().contains("String error");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.nullablemultibranch.infrastructure.avro.ResultSchemaFactory")
                .contentsAsUtf8String().contains("Schema.createUnion");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.nullablemultibranch.infrastructure.avro.SuccessResultToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("event.avsc.nullablemultibranch.infrastructure.avro.FailureResultToGenericRecordConverter")
                .isNotNull();
    }
    @Test
    void arrayBranchUnionGeneratesSealedInterfaceWithStringAndListBranches() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/arraybranch/source/ArrayBranchAvsc.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avsc.arraybranch.ArrayBranchAvscEvent")
                .contentsAsUtf8String().contains("Tags tags");
        assertThat(compilation).generatedSourceFile("event.avsc.arraybranch.Tags")
                .contentsAsUtf8String().contains("sealed interface Tags");
        assertThat(compilation).generatedSourceFile("event.avsc.arraybranch.TagsString")
                .contentsAsUtf8String().contains("String value");
        assertThat(compilation).generatedSourceFile("event.avsc.arraybranch.TagsStringList")
                .contentsAsUtf8String().contains("List<String> value");
        assertThat(compilation)
                .generatedSourceFile("event.avsc.arraybranch.infrastructure.avro.TagsSchemaFactory")
                .contentsAsUtf8String().contains("Schema.createUnion");
    }@Test
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

    @Test
    void avscDependencyEventAssertionsAreNotRegeneratedInConsumerModule() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("event/avsc/dependency/source/DependencyAvsc.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("event/avsc/dependencygeneratedconsumer/source/DependencyGeneratedConsumer.java"));

            assertThat(compilation).succeeded();
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/avsc/dependency/DependencyAvscEventAssert.java")));
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/avsc/dependency/Assertions.java")));
        } finally {
            deleteRecursively(dependencyClasspath);
        }
    }

    @Test
    void lowercaseAvscTypeNamesAreCapitalisedInGeneratedJava() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/lowercase/source/LowercaseAvsc.java"));
        assertThat(compilation).succeeded();
        // Record name "lowercaseAvscEvent" must be capitalised to "LowercaseAvscEvent"
        assertThat(compilation).generatedSourceFile("event.avsc.lowercase.LowercaseAvscEvent")
                .contentsAsUtf8String()
                .contains("name = \"lowercaseAvscEvent\"");
        // Enum name "lowercaseCategory" must be capitalised to "LowercaseCategory"
        assertThat(compilation).generatedSourceFile("event.avsc.lowercase.LowercaseCategory")
                .contentsAsUtf8String()
                .contains("name = \"lowercaseCategory\"");
        // Enum values must be preserved
        assertThat(compilation).generatedSourceFile("event.avsc.lowercase.LowercaseCategory")
                .contentsAsUtf8String()
                .contains("ACTIVE");
        // Generated event implements the contract interface
        assertThat(compilation).generatedSourceFile("event.avsc.lowercase.LowercaseAvscEvent")
                .contentsAsUtf8String()
                .contains("implements LowercaseAvsc");
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

    @Test
    void defaultsAvscEvent_builderFieldsAreInitialisedFromAvscDefaults() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avsc/defaults/source/DefaultsAvsc.java"));
        assertThat(compilation).succeeded();
        var generated = assertThat(compilation).generatedSourceFile("event.avsc.defaults.DefaultsAvscEvent")
                .contentsAsUtf8String();

        // String default
        generated.contains("label = \"unknown\"");
        // int default
        generated.contains("count = 0");
        // long default
        generated.contains("total = 0L");
        // double default
        generated.contains("ratio = 0.0");
        // float default
        generated.contains("factor = (float) 1.0");
        // boolean default
        generated.contains("active = true");
        // array default (empty list)
        generated.contains("tags = java.util.List.of()");
        // null default on nullable field
        generated.contains("nickname = null");
        // enum default
        generated.contains("status = event.avsc.defaults.DefaultStatus.PENDING");
        // field with no default must not have an initialiser in the Builder
        generated.contains("String required");
    }
}
