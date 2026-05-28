package be.appify.prefab.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

class GeneratePluginOverrideIntegrationTest {

    @Test
    void disabledPluginSkipsCreateRequestGeneration() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.override;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.Generate;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.service.Reference;
                        import be.appify.prefab.processor.rest.create.CreatePlugin;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        @Generate(plugin = CreatePlugin.class, enabled = false)
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
                        """, "test.override.Order"));

        assertThat(compilation).succeeded();
        org.junit.jupiter.api.Assertions.assertTrue(
                compilation.generatedFiles().stream()
                        .noneMatch(file -> file.getName().endsWith("/test/override/application/CreateOrderRequest.java"))
        );
    }

    @Test
    void targetTestRoutesGeneratedRequestToClassOutput() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.target;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.Generate;
                        import be.appify.prefab.core.annotations.OutputTarget;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.service.Reference;
                        import be.appify.prefab.processor.rest.create.CreatePlugin;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        @Generate(plugin = CreatePlugin.class, target = OutputTarget.TEST)
                        public record Ticket(
                                @Id Reference<Ticket> id,
                                @Version long version,
                                String subject
                        ) {
                            @Create
                            public Ticket(String subject) {
                                this(Reference.create(), 0L, subject);
                            }
                        }
                        """, "test.target.Ticket"));

        assertThat(compilation).succeeded();
        org.junit.jupiter.api.Assertions.assertTrue(
                compilation.generatedFiles().stream()
                        .noneMatch(file -> file.getName().contains("SOURCE_OUTPUT")
                                && file.getName().endsWith("/test/target/application/CreateTicketRequest.java"))
        );
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/target/application/CreateTicketRequest.java")
                .contentsAsUtf8String()
                .contains("record CreateTicketRequest");
    }

    @Test
    void perAggregateOverrideTakesPrecedenceOverProjectOption() {
        var compilation = javac()
                .withOptions("-Aprefab.plugin.create.enabled=false")
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.precedence;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.Generate;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.service.Reference;
                        import be.appify.prefab.processor.rest.create.CreatePlugin;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        @Generate(plugin = CreatePlugin.class, enabled = true)
                        public record Payment(
                                @Id Reference<Payment> id,
                                @Version long version,
                                String customer
                        ) {
                            @Create
                            public Payment(String customer) {
                                this(Reference.create(), 0L, customer);
                            }
                        }
                        """, "test.precedence.Payment"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.precedence.application.CreatePaymentRequest")
                .contentsAsUtf8String()
                .contains("record CreatePaymentRequest");
    }

    @Test
    void motherPluginTargetMainRoutesMothersToSourceOutput() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.mothermain;

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
                        public record Account(
                                @Id Reference<Account> id,
                                @Version long version,
                                String owner
                        ) {
                            @Create
                            public Account(String owner) {
                                this(Reference.create(), 0L, owner);
                            }
                        }
                        """, "test.mothermain.Account"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.mothermain.CreateAccountRequestMother")
                .contentsAsUtf8String()
                .contains("class CreateAccountRequestMother");
        org.junit.jupiter.api.Assertions.assertTrue(
                compilation.generatedFiles().stream()
                        .noneMatch(file -> file.getName().contains("CLASS_OUTPUT")
                                && file.getName().endsWith("/test/mothermain/CreateAccountRequestMother.java"))
        );
    }

    @Test
    void motherPluginDefaultTargetKeepsMothersInClassOutput() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.motherdefault;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.service.Reference;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        public record Invoice(
                                @Id Reference<Invoice> id,
                                @Version long version,
                                String number
                        ) {
                            @Create
                            public Invoice(String number) {
                                this(Reference.create(), 0L, number);
                            }
                        }
                        """, "test.motherdefault.Invoice"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/motherdefault/CreateInvoiceRequestMother.java")
                .contentsAsUtf8String()
                .contains("class CreateInvoiceRequestMother");
    }

    private static JavaFileObject sourceOf(String sourceCode, String fileName) {
        return JavaFileObjects.forSourceString(fileName, sourceCode);
    }
}




