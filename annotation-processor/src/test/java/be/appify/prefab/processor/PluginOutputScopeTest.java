package be.appify.prefab.processor;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginOutputScopeTest {

    @Test
    void motherPluginMainOverrideRemainsMainWhenDefaultAggregatesExist() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        JavaFileObjects.forSourceString("test.scope.DefaultInvoice", """
                                package test.scope;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.Create;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                public record DefaultInvoice(
                                        @Id Reference<DefaultInvoice> id,
                                        @Version long version,
                                        String number
                                ) {
                                    @Create
                                    public DefaultInvoice(String number) {
                                        this(Reference.create(), 0L, number);
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceString("test.scope.MainAccount", """
                                package test.scope;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.Generate;
                                import be.appify.prefab.core.annotations.OutputTarget;
                                import be.appify.prefab.core.annotations.rest.Create;
                                import be.appify.prefab.core.service.Reference;
                                import be.appify.prefab.processor.mother.MotherPlugin;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                @Generate(plugin = MotherPlugin.class, target = OutputTarget.MAIN)
                                public record MainAccount(
                                        @Id Reference<MainAccount> id,
                                        @Version long version,
                                        String owner
                                ) {
                                    @Create
                                    public MainAccount(String owner) {
                                        this(Reference.create(), 0L, owner);
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.scope.CreateMainAccountRequestMother")
                .contentsAsUtf8String()
                .contains("class CreateMainAccountRequestMother");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/scope/CreateDefaultInvoiceRequestMother.java")
                .contentsAsUtf8String()
                .contains("class CreateDefaultInvoiceRequestMother");
        assertTrue(compilation.generatedFiles().stream()
                .noneMatch(file -> file.getName().contains("CLASS_OUTPUT")
                        && file.getName().endsWith("/test/scope/CreateMainAccountRequestMother.java")));
    }
}


