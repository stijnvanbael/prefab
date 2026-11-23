package be.appify.prefab.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.google.common.truth.Truth.assertThat;

class PluginContributionsTest {

    private static final String SERVICE = "test/contributions/application/OrderService.java";

    @Test
    @DisplayName("plugins run in ascending order() regardless of discovery order")
    void pluginsRunInDeclaredOrder() {
        var forward = compile(List.of(new LatePlugin(), new EarlyPlugin()));
        var reversed = compile(List.of(new EarlyPlugin(), new LatePlugin()));

        assertThat(forward).succeeded();
        var service = generatedSource(forward, SERVICE);
        assertThat(service.indexOf("early()")).isLessThan(service.indexOf("late()"));
        assertThat(generatedSources(reversed)).isEqualTo(generatedSources(forward));
    }

    @Test
    @DisplayName("plugins with the same order() run in class-name order regardless of discovery order")
    void equalOrderIsBrokenByClassName() {
        var forward = compile(List.of(new ZuluPlugin(), new AlphaPlugin()));
        var reversed = compile(List.of(new AlphaPlugin(), new ZuluPlugin()));

        assertThat(forward).succeeded();
        var service = generatedSource(forward, SERVICE);
        assertThat(service.indexOf("alpha()")).isLessThan(service.indexOf("zulu()"));
        assertThat(generatedSources(reversed)).isEqualTo(generatedSources(forward));
    }

    @Test
    @DisplayName("a plugin that overrides no hook leaves every generated file unchanged")
    void noOpPluginChangesNothing() {
        var withNoOp = new ArrayList<>(discoveredPlugins());
        withNoOp.add(new NoOpPlugin());

        var baseline = compile(discoveredPlugins());
        var extended = compile(withNoOp);

        assertThat(baseline).succeeded();
        assertThat(extended).succeeded();
        assertThat(generatedSources(extended)).isEqualTo(generatedSources(baseline));
    }

    @Test
    @DisplayName("recompiling the same sources regenerates identical files")
    void regenerationIsStable() {
        var first = compile(discoveredPlugins());
        var second = compile(discoveredPlugins());

        assertThat(generatedSources(second)).isEqualTo(generatedSources(first));
    }

    @Test
    @DisplayName("two plugins adding the same method fail with an error naming both plugins")
    void duplicateMethodNamesBothPlugins() {
        var compilation = compile(List.of(new AlphaPlugin(), new DuplicateAlphaPlugin()));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Plugin " + DuplicateAlphaPlugin.class.getName()
                + " adds method alpha() to generated OrderService, but " + AlphaPlugin.class.getName()
                + " already contributes it");
    }

    @Test
    @DisplayName("a plugin redefining a generator member fails with an error naming Prefab")
    void duplicateGeneratorMemberNamesPrefab() {
        var compilation = compile(List.of(new LoggerPlugin()));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("adds field log to generated OrderService, but "
                + PluginContributions.GENERATOR + " already contributes it");
    }

    @Test
    @DisplayName("a plugin redefining the parent lookup of a child repository fails with an error naming Prefab")
    void duplicateParentLookupNamesPrefab() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor(List.of(new ParentLookupPlugin())))
                .compile(
                        JavaFileObjects.forSourceString("test.contributions.Basket", """
                                package test.contributions;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                public record Basket(@Id Reference<Basket> id, @Version long version) {
                                }
                                """),
                        JavaFileObjects.forSourceString("test.contributions.BasketLine", """
                                package test.contributions;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.Parent;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                public record BasketLine(
                                        @Id Reference<BasketLine> id,
                                        @Version long version,
                                        @Parent Reference<Basket> basket
                                ) {
                                }
                                """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("adds method findByBasket(java.lang.String, "
                + "org.springframework.data.domain.Pageable) to generated BasketLineRepository, but "
                + PluginContributions.GENERATOR + " already contributes it");
    }

    @Test
    @DisplayName("overloads with distinct parameter types do not conflict")
    void overloadsDoNotConflict() {
        var compilation = compile(List.of(new AlphaPlugin(), new AlphaOverloadPlugin()));

        assertThat(compilation).succeeded();
    }

    private static Compilation compile(List<PrefabPlugin> plugins) {
        return javac()
                .withProcessors(new PrefabProcessor(plugins))
                .compile(JavaFileObjects.forSourceString("test.contributions.Order", """
                        package test.contributions;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.annotations.rest.GetById;
                        import be.appify.prefab.core.service.Reference;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        @GetById
                        public record Order(
                                @Id Reference<Order> id,
                                @Version long version,
                                String customer
                        ) {
                            @Create
                            public Order(String customer) {
                                this(Reference.create(), 0L, customer);
                            }
                        }
                        """));
    }

    private static List<PrefabPlugin> discoveredPlugins() {
        return ServiceLoader.load(PrefabPlugin.class, PrefabProcessor.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    private static Map<String, String> generatedSources(Compilation compilation) {
        return compilation.generatedFiles().stream()
                .filter(file -> file.getKind() == JavaFileObject.Kind.SOURCE)
                .collect(Collectors.toMap(JavaFileObject::getName, PluginContributionsTest::contentOf));
    }

    private static String generatedSource(Compilation compilation, String path) {
        return compilation.generatedFiles().stream()
                .filter(file -> file.getName().endsWith(path))
                .findFirst()
                .map(PluginContributionsTest::contentOf)
                .orElseThrow(() -> new AssertionError("Not generated: " + path));
    }

    private static String contentOf(JavaFileObject file) {
        try {
            return file.getCharContent(true).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static MethodSpec method(String name) {
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC).build();
    }

    static class EarlyPlugin implements PrefabPlugin {
        @Override
        public int order() {
            return -10;
        }

        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addMethod(method("early"));
        }
    }

    static class LatePlugin implements PrefabPlugin {
        @Override
        public int order() {
            return 10;
        }

        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addMethod(method("late"));
        }
    }

    static class AlphaPlugin implements PrefabPlugin {
        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addMethod(method("alpha"));
        }
    }

    static class ZuluPlugin implements PrefabPlugin {
        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addMethod(method("zulu"));
        }
    }

    static class DuplicateAlphaPlugin implements PrefabPlugin {
        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addMethod(method("alpha"));
        }
    }

    static class AlphaOverloadPlugin implements PrefabPlugin {
        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addMethod(MethodSpec.methodBuilder("alpha")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, "reason")
                    .build());
        }
    }

    static class LoggerPlugin implements PrefabPlugin {
        @Override
        public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
            builder.addField(FieldSpec.builder(String.class, "log", Modifier.PRIVATE).build());
        }
    }

    static class ParentLookupPlugin implements PrefabPlugin {
        @Override
        public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
            manifest.parent().ifPresent(parent -> builder.addMethod(MethodSpec.methodBuilder("findByBasket")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(String.class, "basket")
                    .addParameter(Pageable.class, "pageable")
                    .returns(Object.class)
                    .build()));
        }
    }

    static class NoOpPlugin implements PrefabPlugin {
    }
}
