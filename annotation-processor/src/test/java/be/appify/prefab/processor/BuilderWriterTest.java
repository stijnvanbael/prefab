package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import com.google.testing.compile.JavaFileObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class BuilderWriterTest {

    @Test
    void builderWithZeroFieldsContainsOnlyBuildMethod() {
        var code = generateCode("EmptyRecord", List.of());

        assertTrue(code.contains("public static class Builder"), "Expected nested Builder class");
        assertTrue(code.contains("builder()"), "Expected static builder() factory");
        assertTrue(code.contains("public EmptyRecord build()"), "Expected build() method");
        assertFalse(code.contains("withA"), "Expected no setter methods for zero fields");
    }

    @Test
    void builderWithOneFieldHasFluentSetter() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "name").build());
        var code = generateCode("NameRecord", fields);

        assertTrue(code.contains("public static class Builder"), "Expected nested Builder class");
        assertTrue(code.contains("public SELF withName("), "Expected withName setter");
        assertTrue(code.contains("public NameRecord build()"), "Expected build() returning record type");
    }

    @Test
    void builderWithMultipleFieldsHasOneSetterPerField() {
        var fields = List.of(
                ParameterSpec.builder(ClassName.get(String.class), "name").build(),
                ParameterSpec.builder(TypeName.INT, "age").build(),
                ParameterSpec.builder(TypeName.BOOLEAN, "active").build()
        );
        var code = generateCode("PersonRecord", fields);

        assertTrue(code.contains("public SELF withName("), "Expected withName setter");
        assertTrue(code.contains("public SELF withAge("), "Expected withAge setter");
        assertTrue(code.contains("public SELF withActive("), "Expected withActive setter");
    }

    @Test
    void builderSetterStoresFieldAndReturnsBuilder() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "value").build());
        var code = generateCode("SimpleRecord", fields);

        assertTrue(code.contains("this.value = value"), "Expected field assignment in setter");
        assertTrue(code.contains("return self()"), "Expected return self() in setter");
    }

    @Test
    void buildMethodCallsCanonicalConstructor() {
        var fields = List.of(
                ParameterSpec.builder(ClassName.get(String.class), "firstName").build(),
                ParameterSpec.builder(ClassName.get(String.class), "lastName").build()
        );
        var code = generateCode("FullNameRecord", fields);

        assertTrue(code.contains("new FullNameRecord(firstName, lastName)"), "Expected canonical constructor call");
    }

    @Test
    void emptyPrefixProducesPrefixlessSetterNames() {
        var fields = List.of(
                ParameterSpec.builder(ClassName.get(String.class), "name").build(),
                ParameterSpec.builder(TypeName.INT, "age").build()
        );
        var code = generateCodeWithPrefix("PersonRecord", fields, "");

        assertTrue(code.contains("public SELF name("), "Expected prefix-less name() setter");
        assertTrue(code.contains("public SELF age("), "Expected prefix-less age() setter");
        assertFalse(code.contains("withName"), "Expected no withName setter when prefix is empty");
    }

    @Test
    void customPrefixIsUsedForSetterNames() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "name").build());
        var code = generateCodeWithPrefix("NameRecord", fields, "set");

        assertTrue(code.contains("public SELF setName("), "Expected setName setter for 'set' prefix");
        assertFalse(code.contains("withName"), "Expected no withName setter when prefix is 'set'");
    }

    @Test
    void listFieldsGetAddMethodWithNullInitialisationAndFluentReturn() {
        var listOfStrings = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        var fields = List.of(ParameterSpec.builder(listOfStrings, "tags").build());
        var code = generateCode("ListRecord", fields);

        assertTrue(code.contains("public SELF addTags(String item)"), "Expected addTags(String item) method");
        assertTrue(code.contains("if (this.tags == null)"), "Expected null-initialisation in addTags");
        assertTrue(code.contains("this.tags = new ArrayList<>()"), "Expected ArrayList initialisation in addTags");
        assertTrue(code.contains("return self()"), "Expected fluent return in addTags");
    }

    @Test
    void addMethodSupportsRepeatedCallsAndMixedWithSetterUsageAtRuntime() {
        var listOfStrings = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        var fields = List.of(ParameterSpec.builder(listOfStrings, "tags").build());
        var source = generateCode("ListRecord", fields);
        var outputDirectory = compileToClasspath(source);

        try (var classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()},
                Thread.currentThread().getContextClassLoader())) {
            var recordClass = classLoader.loadClass("com.example.ListRecord");
            var builderClass = classLoader.loadClass("com.example.ListRecord$Builder");
            var builderFactory = recordClass.getMethod("builder");
            var addTags = builderClass.getMethod("addTags", String.class);
            var withTags = builderClass.getMethod("withTags", List.class);
            var build = builderClass.getMethod("build");
            var tagsAccessor = recordClass.getMethod("tags");

            var repeatedAddBuilder = builderFactory.invoke(null);
            addTags.invoke(repeatedAddBuilder, "one");
            addTags.invoke(repeatedAddBuilder, "two");
            var repeatedAddRecord = build.invoke(repeatedAddBuilder);
            assertEquals(List.of("one", "two"), tagsAccessor.invoke(repeatedAddRecord));

            var mixedBuilder = builderFactory.invoke(null);
            withTags.invoke(mixedBuilder, List.of("seed"));
            addTags.invoke(mixedBuilder, "next");
            var mixedRecord = build.invoke(mixedBuilder);
            assertEquals(List.of("seed", "next"), tagsAccessor.invoke(mixedRecord));
        } catch (ReflectiveOperationException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteRecursively(outputDirectory);
        }
    }

    @Test
    void toBuilderCopiesValuesAndIsolatesMutableLists() {
        var listOfStrings = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        var fields = List.of(
                ParameterSpec.builder(listOfStrings, "tags").build(),
                ParameterSpec.builder(ClassName.get(String.class), "name").build(),
                ParameterSpec.builder(TypeName.INT, "count").build());
        var outputDirectory = compileToClasspath(generateCodeWithDefaults("ListRecord", fields, "",
                Map.of("name", "\"default\"", "count", "42")));
        try (var loader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()},
                Thread.currentThread().getContextClassLoader())) {
            var recordClass = loader.loadClass("com.example.ListRecord");
            var builderClass = loader.loadClass("com.example.ListRecord$Builder");
            var tags = new ArrayList<>(List.of("original"));
            var original = recordClass.getConstructor(List.class, String.class, int.class)
                    .newInstance(tags, null, 7);
            var toBuilder = recordClass.getMethod("toBuilder");
            var first = toBuilder.invoke(original);
            var second = toBuilder.invoke(original);
            var build = builderClass.getMethod("build");
            assertEquals(original, build.invoke(first));
            builderClass.getMethod("addTags", String.class).invoke(first, "added");
            builderClass.getMethod("name", String.class).invoke(first, "updated");
            var changed = build.invoke(first);
            assertEquals(List.of("original"), tags);
            assertEquals(List.of("original"), recordClass.getMethod("tags").invoke(build.invoke(second)));
            assertEquals(List.of("original", "added"), recordClass.getMethod("tags").invoke(changed));
            assertEquals("updated", recordClass.getMethod("name").invoke(changed));
            assertEquals(7, recordClass.getMethod("count").invoke(changed));
            var nullable = recordClass.getConstructor(List.class, String.class, int.class)
                    .newInstance(null, null, 0);
            assertEquals(nullable, build.invoke(toBuilder.invoke(nullable)));
        } catch (ReflectiveOperationException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteRecursively(outputDirectory);
        }
    }

    @Test
    void toBuilderIsGeneratedForEmptyRecordsWithCustomPrefix() {
        var code = generateCodeWithPrefix("EmptyRecord", List.of(), "set");
        assertTrue(code.contains("toBuilder()"));
    }

    @Test
    void conflictingComponentProducesCompilerDiagnostic() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "toBuilder").build());
        var source = generateCode("ListRecord", fields);
        var compilation = javac().compile(JavaFileObjects.forSourceString("com.example.ListRecord", source));
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("invalid accessor method in record");
    }

    private String generateCode(String recordName, List<ParameterSpec> fields) {
        return generateCodeWithPrefix(recordName, fields, "with");
    }

    private String generateCodeWithPrefix(String recordName, List<ParameterSpec> fields, String prefix) {
        return generateCodeWithDefaults(recordName, fields, prefix, Map.of());
    }

    private String generateCodeWithDefaults(String recordName, List<ParameterSpec> fields, String prefix,
            Map<String, String> defaults) {
        var recordType = ClassName.get("com.example", recordName);
        var recordBuilder = TypeSpec.recordBuilder(recordName)
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder().addParameters(fields).build());
        new BuilderWriter(prefix).enrichWithBuilder(recordBuilder, recordType, fields, defaults);
        return JavaFile.builder("com.example", recordBuilder.build()).build().toString();
    }

    private static Path compileToClasspath(String source) {
        try {
            var outputDirectory = Files.createTempDirectory("prefab-builder-writer-test");
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("System Java compiler is not available");
            }
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var sourceObject = JavaFileObjects.forSourceString("com.example.ListRecord", source);
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null,
                    StandardCharsets.UTF_8)) {
                fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDirectory));
                var options = List.of("-classpath", System.getProperty("java.class.path"));
                var task = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(sourceObject));
                if (!Boolean.TRUE.equals(task.call())) {
                    var errors = diagnostics.getDiagnostics().stream().map(Object::toString).toList();
                    throw new IllegalStateException("Compilation failed: " + String.join("\n", errors));
                }
            }
            return outputDirectory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
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
