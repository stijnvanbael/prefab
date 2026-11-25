package be.appify.prefab.processor;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ConformanceTestPluginTest {

    @Test
    void generatesSecurityAndEventRoundTripTests() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        JavaFileObjects.forSourceString("be.appify.prefab.test.IntegrationTest", """
                                package be.appify.prefab.test;

                                import java.lang.annotation.ElementType;
                                import java.lang.annotation.Target;

                                @Target(ElementType.TYPE)
                                public @interface IntegrationTest {
                                }
                                """),
                        JavaFileObjects.forSourceString("test.conformance.Invoice", """
                                package test.conformance;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.Create;
                                import be.appify.prefab.core.annotations.rest.GetById;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                @GetById
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
                                """),
                        JavaFileObjects.forSourceString("test.conformance.InvoiceEvent", """
                                package test.conformance;

                                import be.appify.prefab.core.annotations.Event;

                                @Event(topic = "invoice-events")
                                public sealed interface InvoiceEvent permits InvoiceEvent.Created {
                                    record Created(String number) implements InvoiceEvent {
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/conformance/InvoiceSecurityConformanceTest.java")
                .contentsAsUtf8String()
                .contains("Anonymous request to secured endpoint");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/conformance/InvoiceSecurityConformanceTest.java")
                .contentsAsUtf8String()
                .contains("assertEndpointSecurity(\"POST\", \"/invoices\", false, \"\", \"\")");

        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/conformance/InvoiceEventEventConformanceTest.java")
                .contentsAsUtf8String()
                .contains("InvoiceEventCreatedMother.createInvoiceEventCreated()");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/conformance/InvoiceEventEventConformanceTest.java")
                .contentsAsUtf8String()
                .contains("codec.encode(\"invoice-events\", expected).decode()");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "test/conformance/InvoiceEventEventConformanceTest.java")
                .contentsAsUtf8String()
                .contains("Event InvoiceEvent should round-trip through the platform serializer");
    }

    @Test
    void aggregateAndEventCanOptOutOfTheirConformanceTests() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        JavaFileObjects.forSourceString("be.appify.prefab.test.IntegrationTest", """
                                package be.appify.prefab.test;

                                import java.lang.annotation.ElementType;
                                import java.lang.annotation.Target;

                                @Target(ElementType.TYPE)
                                public @interface IntegrationTest {
                                }
                                """),
                        JavaFileObjects.forSourceString("test.optout.DisabledAggregate", """
                                package test.optout;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.Create;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate(conformanceTest = false)
                                public record DisabledAggregate(
                                        @Id Reference<DisabledAggregate> id,
                                        @Version long version,
                                        String name
                                ) {
                                    @Create
                                    public DisabledAggregate(String name) {
                                        this(Reference.create(), 0L, name);
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceString("test.optout.DisabledEvent", """
                                package test.optout;

                                import be.appify.prefab.core.annotations.Event;

                                @Event(topic = "disabled-events", conformanceTest = false)
                                public record DisabledEvent(String name) {
                                }
                                """));

        assertThat(compilation).succeeded();
        org.junit.jupiter.api.Assertions.assertTrue(compilation.generatedFiles().stream()
                .noneMatch(file -> file.getName().contains("DisabledAggregateSecurityConformanceTest")));
        org.junit.jupiter.api.Assertions.assertTrue(compilation.generatedFiles().stream()
                .noneMatch(file -> file.getName().contains("DisabledEventEventConformanceTest")));
    }
}
